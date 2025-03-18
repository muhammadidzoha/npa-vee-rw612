/*
 * C
 *
 * Copyright 2015-2024 MicroEJ Corp. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be found with this software.
 *
 * Copyright 2024-2025 NXP
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

/**
 * @file
 * @brief lwip_util implementation over LWIP and FreeRTOS.
 * @author MicroEJ Developer Team
 * @version 0.2.1
 * @date 3 August 2020
 */
#include "lwip/dhcp.h"
#include "lwip/ip_addr.h"
#include "lwip/prot/dhcp.h"
#include "LLNET_Common.h"

#include "lwip/opt.h"
#include "lwip/init.h"
#include "lwip/netif.h"
#include "lwip/dhcp.h"
#include "lwip/dns.h"
#include "netif/etharp.h"
#include "lwip/netdb.h"
#include "lwip/netifapi.h"
#include "lwip/tcpip.h"
#include "netif/ethernet.h"
#include <stddef.h>
#include <string.h>
#include <stdio.h>
#include "lwip/netifapi.h"
#include "arch/sys_arch.h"
#include "pin_mux.h"
#include "board.h"
#include "lwip/apps/lwiperf.h"
#ifdef ENABLE_ETHERNET
#include "fsl_silicon_id.h"
#include "fsl_phy.h"
#include "ethernetif.h"
#include "fsl_phyksz8081.h"
#include "fsl_adapter_gpio.h"
#include "fsl_gpio.h"
#include "fsl_enet.h"
#include "fsl_adapter_gpio.h"
#endif

#include "fsl_component_serial_manager.h"
#include "lwip/sockets.h"


#include "fsl_debug_console.h"
#ifdef ENABLE_WIFI
#include "WIFI_RW610_driver.h"
#endif


static TaskHandle_t	dhcp_task_handle;

/*******************************************************************************
 * Definitions
 ******************************************************************************/

#ifdef ENABLE_ETHERNET
phy_ksz8081_resource_t g_phy_resource;
/* PHY operations. */
#define ENET_PHY_OPS      &phyksz8081_ops
/* ENET instance select. */
#define ETHERNETIF_INIT ethernetif0_init
/* ENET clock frequency */
#define ENET_CLOCK_FREQ CLOCK_GetMainClkFreq()
#define ENET_PHY_RESOURCE &g_phy_resource
/* Address of PHY interface. */
#define ENET_PHY_ADDRESS BOARD_ENET0_PHY_ADDRESS
#endif


char lwip_netif[NUMB_OF_NETIF_TO_STORE][MAX_SIZE_OF_NETIF_NAME];

/*******************************************************************************
 * Prototypes
 ******************************************************************************/

/*******************************************************************************
 * Variables
 ******************************************************************************/
#ifdef ENABLE_ETHERNET
/*Initialize physic etherner handler */
static phy_handle_t phyHandle;

void BOARD_ENETFlexibleConfigure(enet_config_t *config)
{
    config->miiMode = kENET_RmiiMode;
}
#endif

// Sanity check. Synchronization has to be activated in LWIP configuration file.
#if (SYS_LIGHTWEIGHT_PROT != 1)
	#error LWIP synchronization not activated in lwipopts.h file. Please add "#define SYS_LIGHTWEIGHT_PROT	1" in lwipopts.h header file.
#endif

#define MAX_DHCP_TRIES  4
#define LWIP_DHCP_TASK_PRIORITY 1

#define LWIP_DHCP_POLLING_INTERVAL 250

/* DHCP process states */
#define DHCP_START                 (uint8_t) 1
#define DHCP_WAIT_ADDRESS          (uint8_t) 2
#define DHCP_ADDRESS_ASSIGNED      (uint8_t) 3
#define DHCP_TIMEOUT               (uint8_t) 4
#define DHCP_LINK_DOWN             (uint8_t) 5

struct netif gnetif;
uint8_t DHCP_state;


static uint8_t dhcp_sleeping = 1;

/**
 * @brief Link status callback function.
 *
 * @param netif_ the network interface which status has changed.
 * @param reason the reason for status change.
 * @param args additionnal infos on status change.
 */
static void netif_link_status_callback(struct netif *netif_, netif_nsc_reason_t reason, const netif_ext_callback_args_t *args);

/* variable used to notify that DNS servers list has changed */
uint8_t dns_servers_list_updated = 1;


static void ethernetif_static_ip_config(void);

static netif_ext_callback_t g_link_status_callback_info;

/**
  * @brief  Reset the network interface ip, netmask and gateway addresses to zero.
  * @param  netif: the network interface
  * @retval None
  */
static void netif_addr_set_zero_ip4(struct netif* netif){
	ip_addr_set_zero_ip4(&netif->ip_addr);
	ip_addr_set_zero_ip4(&netif->netmask);
	ip_addr_set_zero_ip4(&netif->gw);
}

/**
  * @brief  This function is called when the network interface is disconnected.
  * @param  netif: the network interface
  * @retval None
  */
void netif_not_connected(struct netif *netif){
	netif_addr_set_zero_ip4(netif);
	LLNET_DEBUG_TRACE("[INFO] The network cable is not connected \n");
}

/**
  * @brief  Notify the User about the nework interface config status
  * @param  netif: the network interface
  * @retval None
  */
static void User_notification(struct netif *netif)
{
	int32_t dhcpConfEnabled = true;
	if (netif_is_up(netif))
	{
		if(dhcpConfEnabled)
		{
			/* Update DHCP state machine */
			DHCP_state = DHCP_START;
		}else{
			// launch static Network Interface configuration
			ethernetif_static_ip_config();
		}
	}
	else
	{
		netif_not_connected(netif);
		if(dhcpConfEnabled)
		{
			/* Update DHCP state machine */
			DHCP_state = DHCP_LINK_DOWN;
		}
	}

}

#ifdef ENABLE_ETHERNET
static void MDIOInit(void)
{
    uint32_t i = ENET_GetInstance(ENET);
    (void)CLOCK_EnableClock(s_enetClock[i]);
    (void)CLOCK_EnableClock(s_enetExtraClock[i]);
    ENET_SetSMI(ENET, ENET_CLOCK_FREQ, false);
}

static status_t MDIOWrite(uint8_t phyAddr, uint8_t regAddr, uint16_t data)
{
    return ENET_MDIOWrite(ENET, phyAddr, regAddr, data);
}

static status_t MDIORead(uint8_t phyAddr, uint8_t regAddr, uint16_t *pData)
{
    return ENET_MDIORead(ENET, phyAddr, regAddr, pData);
}

/**
 * @brief  Setup the network interface
 * @param  None
 * @retval None
 */
static void Netif_Config(void)
{
	ip_addr_t ipaddr;
	ip_addr_t netmask;
	ip_addr_t gw;

	ip_addr_set_zero_ip4(&ipaddr);
	ip_addr_set_zero_ip4(&netmask);
	ip_addr_set_zero_ip4(&gw);
    MDIOInit();
    g_phy_resource.read  = MDIORead;
    g_phy_resource.write = MDIOWrite;
    ethernetif_config_t enet_config = {.phyHandle   = &phyHandle,
                                       .phyAddr     = ENET_PHY_ADDRESS,
                                       .phyOps      = ENET_PHY_OPS,
                                       .phyResource = ENET_PHY_RESOURCE,
                                       .srcClockHz  = ENET_CLOCK_FREQ,
#ifdef configMAC_ADDR
                                       .macAddress = configMAC_ADDR
#endif
    };
#ifndef configMAC_ADDR
    /* Set special address for each chip. */
    (void)SILICONID_ConvertToMacAddr(&enet_config.macAddress);
#endif

	/* Add the network interface */
#if NO_SYS
	netifapi_netif_add(&gnetif, &ipaddr, &netmask, &gw, &enet_config, ETHERNETIF_INIT, &netif_input);
#else
	netifapi_netif_add(&gnetif, &ipaddr, &netmask, &gw,  &enet_config, ETHERNETIF_INIT, &tcpip_input);

#endif

	/* Registers the default network interface */
	netifapi_netif_set_default(&gnetif);
	netifapi_netif_set_up(netif_default);
	/* Set the link callback function, this function is called on change of link status */
	//netif_set_link_callback(&gnetif, ethernetif_config);
    netif_add_ext_callback(&g_link_status_callback_info, netif_link_status_callback);
	User_notification(&gnetif);
}
#endif

/*
 * Retrieve static IP configuration of the default network interface and set
 * IP parameters (Interface IP address, Netmask and Gateway IP address).
 */
static void ethernetif_static_ip_config()
{
	ip_addr_t ipaddr;
	ip_addr_t netmask;
	ip_addr_t gw;
	ip_addr_t dnsaddr;
	PRINTF("getting static address \n");
	// static IP configuration. Retrieve IP settings from user properties.
	ipaddr.addr = 0;
	netmask.addr = 0;
	gw.addr = 0;
	netif_set_addr(&gnetif, &ipaddr , &netmask, &gw);
	LLNET_DEBUG_TRACE("[INFO] Static IP address assigned: %s\n", inet_ntoa(ipaddr.addr));

	// set static DNS Host IP address.
	if(DNS_MAX_SERVERS > 0)
	{
		char * static_dns_ip_addr = NULL;
		if(static_dns_ip_addr != NULL)
		{
			inet_pton(AF_INET, (char *)dnsaddr.addr, static_dns_ip_addr);
			dns_setserver(0, &dnsaddr);
			// notify DNS servers IP address updated
			dns_servers_list_updated = 1;
		}
	}
}

/**
  * @brief  Link status callback function.
  * @param  netif_ the network interface which status has changed.
  * @param  reason the reason for status change.
  * @param  args additional infos on status change.
  * @retval None
  */
static void netif_link_status_callback(struct netif *netif_, netif_nsc_reason_t reason,
                                       const netif_ext_callback_args_t *args)
{
	ip_addr_t ipaddr = { 0 };
	ip_addr_t netmask = { 0 };
	ip_addr_t gw = { 0 };
    int32_t dhcp_conf_enabled = true;

    LLNET_DEBUG_TRACE("%s: reason=%d, args->link_changed.state=%d, netif_->num=%d\n", __func__, reason, args->link_changed.state, netif_->num);
    if (LWIP_NSC_LINK_CHANGED == reason) {
        if (args->link_changed.state) {
            if (dhcp_conf_enabled) {
                LLNET_DEBUG_TRACE("%s: The network cable is now connected\n", __func__);
                // Update DHCP state machine
                DHCP_state = DHCP_START;
                netif_set_addr(netif_, &ipaddr , &netmask, &gw);
                // Resume DHCP thread.
                dhcp_sleeping = 0;
                vTaskResume(dhcp_task_handle);
                netif_set_up(netif_);
            } else {
                // Launch static network interface configuration.
                ethernetif_static_ip_config();
            }
        } else {
            LLNET_DEBUG_TRACE("%s: The network cable is now disconnected.\n", __func__);
            if (dhcp_conf_enabled) {
                // Update DHCP state machine.
                DHCP_state = DHCP_LINK_DOWN;
            }
            // Link is down, set the interface down as well.
            netif_set_down(netif_);
            netif_not_connected(netif_);
		}
	}
}

static void DHCP_thread(void const * argument)
{
  struct netif *netif = (struct netif *) argument;
  uint32_t IPaddress;

  for (;;)
  {
  	// check if DHCP thread has to suspend
	if(dhcp_sleeping == 1){
		vTaskDelete(dhcp_task_handle);
	}

    switch (DHCP_state)
    {
    case DHCP_START:
      {
        netif_addr_set_zero_ip4(netif);
        IPaddress = 0;
        netifapi_dhcp_start(netif);
        DHCP_state = DHCP_WAIT_ADDRESS;
        LLNET_DEBUG_TRACE("[INFO] DHCP started\n");
      }
      break;

    case DHCP_WAIT_ADDRESS:
      {
        /* Read the new IP address */
        IPaddress = netif->ip_addr.addr;
        if (IPaddress != 0) {
			DHCP_state = DHCP_ADDRESS_ASSIGNED;

			/* Stop DHCP */
#if LWIP_VERSION_MAJOR == 1
			dhcp_stop(netif);
#elif LWIP_VERSION_MAJOR == 2
          	// LwIP version 2 onward clears existing IP address if dhcp is stopped.
          	//dhcp_stop(netif);
#else
			#error "Invalid LWIP version (LWIP_VERSION_MAJOR)."
#endif
			dhcp_sleeping = 1;

			LLNET_DEBUG_TRACE("[INFO] DHCP address assigned: %s\n", ip4addr_ntoa(netif_ip4_addr(netif)));
			if(1)
			{
				ip_addr_t dnsaddr;
				if(DNS_MAX_SERVERS > 0)
				{
					char * static_dns_ip_addr = NULL;
					if(static_dns_ip_addr != NULL)
					{
						inet_pton(AF_INET, (char *)dnsaddr.addr, static_dns_ip_addr);
						dns_setserver(0, &dnsaddr);
					}
				}
			}

			// notify DNS servers IP address updated
			dns_servers_list_updated = 1;
        }
        else
        {
#if LWIP_VERSION_MAJOR == 1

        	struct dhcp *dhcp = netif->dhcp;

#elif LWIP_VERSION_MAJOR == 2
			struct dhcp *dhcp = (struct dhcp*)netif_get_client_data(netif, LWIP_NETIF_CLIENT_DATA_INDEX_DHCP);

#else

	#error "Invalid LWIP version (LWIP_VERSION_MAJOR)."

#endif

			/* DHCP timeout */
			if (dhcp->tries > MAX_DHCP_TRIES)
			{
				DHCP_state = DHCP_TIMEOUT;

				/* Stop DHCP */
				dhcp_stop(netif);
				dhcp_sleeping = 1;

				LLNET_DEBUG_TRACE("[INFO] DHCP timeout\n");
			}
        }
      }
      break;

    default: break;
    }
    // Stop polling for 250 ms.
    vTaskDelay(pdMS_TO_TICKS(LWIP_DHCP_POLLING_INTERVAL));
  }
}

/**
 * Network initialization. Start network interfaces and configure it.
 * @return 0 if no error occurred, error code otherwise.
 */
int32_t llnet_lwip_init(void)
{
	PRINTF ("network initialize \n");
#ifdef ENABLE_ETHERNET
	gpio_pin_config_t gpio_config = {kGPIO_DigitalOutput, 1U};
	ENET_ResetHardware();
	GPIO_PortInit(GPIO, 0U);
	GPIO_PortInit(GPIO, 1U);
	GPIO_PinInit(GPIO, 0U, 21U, &gpio_config); /* ENET_RST */
	gpio_config.pinDirection = kGPIO_DigitalInput;
	gpio_config.outputLogic  = 0U;
	GPIO_PinInit(GPIO, 1U, 23U, &gpio_config); /* ENET_INT */
	GPIO_PinWrite(GPIO, 0U, 21U, 0U);
	SDK_DelayAtLeastUs(1000000, CLOCK_GetCoreSysClkFreq());
	GPIO_PinWrite(GPIO, 0U, 21U, 1U);
	HAL_GpioPreInit();
	int32_t dhcpConfEnabled =  true;
	if (!wifi_if_init_done) {
		/* Initialize the LwIP TCP/IP stack */
		tcpip_init(NULL, NULL);
	}
	/* Configure the Network interface */
	Netif_Config();
	if(dhcpConfEnabled)
	{
		/* Start DHCPClient */
		dhcp_sleeping = 0;
#if defined(__GNUC__)
		xTaskCreate((TaskFunction_t)DHCP_thread, "DHCP", configMINIMAL_STACK_SIZE * 5, &gnetif, LWIP_DHCP_TASK_PRIORITY, &dhcp_task_handle);
#else
		xTaskCreate((TaskFunction_t)DHCP_thread, "DHCP", configMINIMAL_STACK_SIZE*2, &gnetif, LWIP_DHCP_TASK_PRIORITY, &dhcp_task_handle);
#endif
	}
#endif
	return 0;
}

void set_lwip_netif_name(int32_t id, char *netif_name)
{
	strcpy(lwip_netif[id], netif_name);
}

char *get_lwip_netif_name(int32_t id)
{
	return lwip_netif[id];
}

struct netif* getNetworkInterface(uint8_t* name)
{
	struct netif *pnetif_list = netif_list;

	while (pnetif_list != NULL) {
		int8_t current_netif[6] = {0};
		sprintf((char *)current_netif, "%c%c%d", pnetif_list->name[0], pnetif_list->name[1], pnetif_list->num);
		if (strcmp((char *)name, (char *)current_netif) == 0) {
			// the same interface name at LLNET level can have different names at platform level
			// so, keep trying to find the lwip level interface until one is found or until we exhaust the list
			struct netif *ret = netif_find(lwip_netif[pnetif_list->num]);
			if (ret) {
				return ret;
			}
		}
		pnetif_list = pnetif_list->next;
	}
	return NULL;
}
