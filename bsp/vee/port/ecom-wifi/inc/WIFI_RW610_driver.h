/*
 * C
 *
 * Copyright 2023 MicroEJ Corp. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be found with this software.
 */

#ifndef WIFI_RW610_DRIVER_H
#define WIFI_RW610_DRIVER_H

#include <stdbool.h>
#include <stdint.h>
#include "lwip/inet.h"

#ifdef __cplusplus
	extern "C" {
#endif

/**
 * @brief function to initialize the wifi module
 *
 * @return true if success else false
 */
bool WIFI_RW610_initialize_f(void);

/**
 * @brief this function apply IP configuration from network interface
 *
 * @param[in] is_static IP configuration, true for static ip or false for dhcp
 * @param[in] ip ip address
 * @param[in] netmask netmask
 * @param[in] gw gateway
 *
 * @return true if success else false
 */
bool WIFI_RW610_sta_apply_configuration_f(bool is_static, ip4_addr_t ip, ip4_addr_t netmask, ip4_addr_t gw);

/**
 * @brief this function start the wifi module in STA mode
 *
 * @return true if success else false
 */
bool WIFI_RW610_sta_start_f(void);

/**
 * @brief this function start the wifi module in AP mode
 *
 * @return true if success else false
 */
bool WIFI_RW610_ap_start_f(void);

/**
 * @brief this function stop the STA wifi module
 *
 * @return true if success else false
 */
bool WIFI_RW610_sta_stop_f(void);

/**
 * @brief this function stop the AP wifi module
 *
 * @return true if success else false
 */
bool WIFI_RW610_ap_stop_f(void);

/**
 * @brief this function return if the STA network interface is started or not
 *
 * @param[out] netif_started '1' returned if started, '0' otherwise
 *
 * @return true if success else false
 */
bool WIFI_RW610_sta_netif_is_started_f(int8_t* netif_started);

/**
 * @brief this function return if the AP network interface is started or not
 *
 * @param[out] netif_started '1' returned if started, '0' otherwise
 *
 * @return true if success else false
 */
bool WIFI_RW610_ap_netif_is_started_f(int8_t* netif_started);

/**
 * @brief this function joins a WiFi network
 *
 * @param[in] ssid ssid of the network
 * @param[in] ssid_length ssid length
 * @param[in] passphrase password needed for network connection
 * @param[in] passphrase_length password length
 * @param[in] security_mode security mode
 *
 * @return true if success else false
 */
bool WIFI_RW610_join_f(int8_t* ssid, int32_t ssid_length, int8_t* passphrase, int32_t passphrase_length, int32_t security_mode);

/**
 * @brief this function leaves a WiFi network
 *
 * @return true if success else false
 */
bool WIFI_RW610_leave_f(void);

/**
 * @brief this function returns the bssid of the WiFi network
 *
 * @param[out] bssid the buffer to be filled with the bssid
 * @param[in]  bssid_length length to be filled in the buffer
 *
 * @return true if success else false
 */
bool WIFI_RW610_get_bssid_f(int8_t* bssid, int32_t bssid_length);

/**
 * @brief this function returns the channel of the WiFi network
 *
 * @param[out] channel the buffer to be filled
 *
 * @return true if success else false
 */
bool WIFI_RW610_get_channel_f(int32_t* channel);

/**
 * @brief this function returns the rssi of the WiFi network
 *
 * @param[out] rssi the buffer to be filled
 * @param[in]  rssi_length length to be filled in the buffer
 *
 * @return true if success else false
 */
bool WIFI_RW610_get_rssi_f(float* rssi, int32_t rssi_length);

/**
 * @brief this function returns the security mode of the WiFi network
 *
 * @param[out] security_mode the buffer to be filled
 *
 * @return true if success else false
 */
bool WIFI_RW610_get_security_mode_f(int32_t* security_mode);

/**
 * @brief this function returns the ssid of the WiFi network
 *
 * @param[out] ssid the buffer to be filled
 * @param[in]  ssid_length length to be filled in the buffer
 *
 * @return true if success else false
 */
bool WIFI_RW610_get_ssid_f(int8_t* ssid, int32_t ssid_length);

/**
 * @brief this function returns the WPS modes of the WiFi network
 *
 * @param[out] wps_modes the buffer to be filled
 *
 * @return true if success else false
 */
bool WIFI_RW610_get_wps_modes_f(int32_t* wps_modes);

/**
 * @brief this function enables the SoftAP for the module
 *
 * @param[in] ssid the ssid of the SoftAP
 * @param[in] ssid_length the ssid length
 * @param[in] passphrase the password of the SoftAP
 * @param[in] passphrase_length the password length
 *
 * @return true if success else false
 */
bool WIFI_RW610_enable_softap_f(int8_t* ssid, int32_t ssid_length, int8_t* passphrase, int32_t passphrase_length);

/**
 * @brief this function changes the name of the SoftAP
 *
 * @param[in] name the ssid of the SoftAP
 * @param[in] name_length the name length
 *
 * @return true if success else false
 */
bool WIFI_RW610_set_name_softap_f(int8_t* name, int32_t name_length);

/**
 * @brief this function disables the SoftAP
 *
 * @return true if success else false
 */
bool WIFI_RW610_disable_softap_f(void);

/**
 * @brief this function returns the access point count discovered
 *
 * @param[out] ap_count the access point count to be returned
 * @param[in]  active true for active scan, false for passive scan
 *
 * @return true if success else false
 */
bool WIFI_RW610_get_ap_count_f(int32_t* ap_count, int8_t active);

/**
 * @brief this function returns the ssid from the saved list
 *
 * @param[in]  index the index from the saved list
 * @param[out] ssid the buffer to be returned
 * @param[in]  ssid_length the length to copy in the return buffer
 *
 * @return true if success else false
 */
bool WIFI_RW610_get_ap_ssid_f(int32_t index, int8_t* ssid, int32_t ssid_length);

/**
 * @brief this function returns the bssid from the saved list
 *
 * @param[in]  index the index from the saved list
 * @param[out] bssid the buffer to be returned
 * @param[in]  bssid_length the length to copy in the return buffer
 *
 * @return true if success else false
 */
bool WIFI_RW610_get_ap_bssid_f(int32_t index, int8_t* bssid, int32_t bssid_length);

/**
 * @brief this function returns the channel from the saved list
 *
 * @param[in]  index the index from the saved list
 * @param[out] channel the buffer to be returned
 *
 * @return true if success else false
 */
bool WIFI_RW610_get_ap_channel_f(int32_t index, int32_t* channel);

/**
 * @brief this function returns the rssi from the saved list
 *
 * @param[in]  index the index from the saved list
 * @param[out] rssi the buffer to be returned
 * @param[in]  rssi_length the length to copy in the return buffer
 *
 * @return true if success else false
 */
bool WIFI_RW610_get_ap_rssi_f(int32_t index, float* rssi, int32_t rssi_length);

/**
 * @brief this function returns the security mode from the saved list
 *
 * @param[in]  index the index from the saved list
 * @param[out] _pi_mode the buffer to be returned
 *
 * @return true if success else false
 */
bool WIFI_RW610_get_ap_security_mode_f(int32_t index, int32_t* security_mode);

/**
 * @brief this function returns the wps mode from the saved list
 *
 * @param[in]  index the index from the saved list
 * @param[out] wps_modes the buffer to be returned
 *
 * @return true if success else false
 */
bool WIFI_RW610_get_ap_wps_modes_f(int32_t index, int32_t* wps_modes);

/**
 * @brief this function returns the station state
 *
 * @return true if the station is started else false
 */
bool WIFI_RW610_get_client_state_f(void);

/**
 * @brief this function returns the access point state
 *
 * @return true if the access point is started else false
 */
bool WIFI_RW610_get_access_point_state_f(void);

#ifdef __cplusplus
	}
#endif

#endif /* WIFI_RW610_DRIVER_H */
