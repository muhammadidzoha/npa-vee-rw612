#include "ds3231.h"

#include "fsl_i2c.h"
#include "fsl_debug_console.h"

#include <stdint.h>
#include <string.h>

#define DS3231_I2C_BASE I2C2
#define DS3231_I2C_CLOCK_HZ 16000000U
#define DS3231_I2C_BAUDRATE 100000U

#define DS3231_I2C_ADDRESS 0x68U
#define DS3231_REGISTER_SECONDS 0x00U

static uint8_t DS3231_BcdToDecimal(uint8_t value)
{
    return (uint8_t)(((value >> 4U) * 10U) + (value & 0x0FU));
}

bool DS3231_Init(void)
{
    i2c_master_config_t masterConfig;

    PRINTF("[RTC] Initializing I2C2...\r\n");

    I2C_MasterGetDefaultConfig(&masterConfig);
    masterConfig.baudRate_Bps = DS3231_I2C_BAUDRATE;

    I2C_MasterInit(DS3231_I2C_BASE, &masterConfig, DS3231_I2C_CLOCK_HZ);

    PRINTF("[RTC] I2C2 initialized | clock=%u Hz | baudrate=%u Hz\r\n", DS3231_I2C_CLOCK_HZ, DS3231_I2C_BAUDRATE);

    return true;
}

bool DS3231_TestCommunication(void)
{
    i2c_master_transfer_t transfer;
    uint8_t secondsRaw = 0U;
    uint8_t seconds;
    status_t status;

    memset(&transfer, 0, sizeof(transfer));

    transfer.slaveAddress = DS3231_I2C_ADDRESS;
    transfer.direction = kI2C_Read;
    transfer.subaddress = DS3231_REGISTER_SECONDS;
    transfer.subaddressSize = 1U;
    transfer.data = &secondsRaw;
    transfer.dataSize = 1U;
    transfer.flags = kI2C_TransferDefaultFlag;

    PRINTF("[RTC] Probing DS3231 at address 0x68...\r\n");

    status = I2C_MasterTransferBlocking(DS3231_I2C_BASE, &transfer);

    if (status != kStatus_Success)
    {
        PRINTF("[RTC] ERROR: DS3231 not detected | status=%d\r\n", (int)status);
        return false;
    }

    secondsRaw &= 0x7FU;
    seconds = DS3231_BcdToDecimal(secondsRaw);

    if (seconds > 59U)
    {
        PRINTF("[RTC] ERROR: Invalid seconds register | raw=0x%02X | decoded=%u\r\n", secondsRaw, seconds);
        return false;
    }

    PRINTF("[RTC] DS3231 detected successfully.\r\n");
    PRINTF("[RTC] Seconds register | raw=0x%02X | decoded=%u\r\n", secondsRaw, seconds);

    return true;
}