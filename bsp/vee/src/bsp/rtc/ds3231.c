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

static bool DS3231_ReadRegister(uint8_t registerAddress, uint8_t *value)
{
    i2c_master_transfer_t transfer;
    status_t status;

    if (value == NULL)
    {
        return false;
    }

    memset(&transfer, 0, sizeof(transfer));

    transfer.slaveAddress = DS3231_I2C_ADDRESS;
    transfer.direction = kI2C_Read;
    transfer.subaddress = registerAddress;
    transfer.subaddressSize = 1U;
    transfer.data = value;
    transfer.dataSize = 1U;
    transfer.flags = kI2C_TransferDefaultFlag;

    status = I2C_MasterTransferBlocking(DS3231_I2C_BASE, &transfer);

    if (status != kStatus_Success)
    {
        PRINTF(
            "[RTC] I2C read failed | register=0x%02X | status=%d\r\n",
            registerAddress,
            (int)status
        );

        return false;
    }

    return true;
}

bool DS3231_Init(void)
{
    i2c_master_config_t masterConfig;

    PRINTF("[RTC] Initializing I2C2...\r\n");

    I2C_MasterGetDefaultConfig(&masterConfig);
    masterConfig.baudRate_Bps = DS3231_I2C_BAUDRATE;

    I2C_MasterInit(
        DS3231_I2C_BASE,
        &masterConfig,
        DS3231_I2C_CLOCK_HZ
    );

    PRINTF(
        "[RTC] I2C2 initialized | clock=%u Hz | baudrate=%u Hz\r\n",
        DS3231_I2C_CLOCK_HZ,
        DS3231_I2C_BAUDRATE
    );

    return true;
}

bool DS3231_TestCommunication(void)
{
    uint8_t secondsRaw = 0U;
    uint8_t seconds;

    PRINTF("[RTC] Probing DS3231 at address 0x68...\r\n");

    if (!DS3231_ReadRegister(DS3231_REGISTER_SECONDS, &secondsRaw))
    {
        PRINTF("[RTC] ERROR: DS3231 not detected.\r\n");
        return false;
    }

    secondsRaw &= 0x7FU;
    seconds = DS3231_BcdToDecimal(secondsRaw);

    if (seconds > 59U)
    {
        PRINTF(
            "[RTC] ERROR: Invalid seconds | raw=0x%02X | decoded=%u\r\n",
            secondsRaw,
            seconds
        );

        return false;
    }

    PRINTF("[RTC] DS3231 detected successfully.\r\n");

    PRINTF(
        "[RTC] Seconds register | raw=0x%02X | decoded=%u\r\n",
        secondsRaw,
        seconds
    );

    return true;
}