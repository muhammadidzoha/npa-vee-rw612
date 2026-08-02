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
#define DS3231_REGISTER_MINUTES 0x01U
#define DS3231_REGISTER_HOURS 0x02U
#define DS3231_REGISTER_DAY 0x03U
#define DS3231_REGISTER_DATE 0x04U
#define DS3231_REGISTER_MONTH 0x05U
#define DS3231_REGISTER_YEAR 0x06U

#define DS3231_REGISTER_STATUS 0x0FU

#define DS3231_STATUS_OSF_MASK 0x80U

static uint8_t DS3231_BcdToDecimal(uint8_t value)
{
    return (uint8_t)(((value >> 4U) * 10U) + (value & 0x0FU));
}

static uint8_t DS3231_DecimalToBcd(uint8_t value)
{
    return (uint8_t)(((value / 10U) << 4U) | (value % 10U));
}

static bool DS3231_ReadRegisters(uint8_t registerAddress, uint8_t *data, size_t dataSize)
{
    i2c_master_transfer_t transfer;
    status_t status;

    if (data == NULL || dataSize == 0U)
    {
        return false;
    }

    memset(&transfer, 0, sizeof(transfer));

    transfer.slaveAddress = DS3231_I2C_ADDRESS;
    transfer.direction = kI2C_Read;
    transfer.subaddress = registerAddress;
    transfer.subaddressSize = 1U;
    transfer.data = data;
    transfer.dataSize = dataSize;
    transfer.flags = kI2C_TransferDefaultFlag;

    status = I2C_MasterTransferBlocking(DS3231_I2C_BASE, &transfer);

    if (status != kStatus_Success)
    {
        PRINTF("[RTC] I2C read failed | register=0x%02X | status=%d\r\n", registerAddress, (int)status);
        return false;
    }

    return true;
}

static bool DS3231_WriteRegisters(uint8_t registerAddress, const uint8_t *data, size_t dataSize)
{
    i2c_master_transfer_t transfer;
    status_t status;

    if (data == NULL || dataSize == 0U)
    {
        return false;
    }

    memset(&transfer, 0, sizeof(transfer));

    transfer.slaveAddress = DS3231_I2C_ADDRESS;
    transfer.direction = kI2C_Write;
    transfer.subaddress = registerAddress;
    transfer.subaddressSize = 1U;
    transfer.data = (uint8_t *)data;
    transfer.dataSize = dataSize;
    transfer.flags = kI2C_TransferDefaultFlag;

    status = I2C_MasterTransferBlocking(DS3231_I2C_BASE, &transfer);

    if (status != kStatus_Success)
    {
        PRINTF("[RTC] I2C write failed | register=0x%02X | status=%d\r\n", registerAddress, (int)status);
        return false;
    }

    return true;
}

static bool DS3231_IsDateTimeRangeValid(const ds3231_datetime_t *dateTime)
{
    if (dateTime == NULL)
    {
        return false;
    }

    if (dateTime->year < 2000U || dateTime->year > 2099U)
    {
        return false;
    }

    if (dateTime->month < 1U || dateTime->month > 12U)
    {
        return false;
    }

    if (dateTime->date < 1U || dateTime->date > 31U)
    {
        return false;
    }

    if (dateTime->day < 1U || dateTime->day > 7U)
    {
        return false;
    }

    if (dateTime->hour > 23U)
    {
        return false;
    }

    if (dateTime->minute > 59U)
    {
        return false;
    }

    if (dateTime->second > 59U)
    {
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

    I2C_MasterInit(DS3231_I2C_BASE, &masterConfig, DS3231_I2C_CLOCK_HZ);

    PRINTF("[RTC] I2C2 initialized | clock=%u Hz | baudrate=%u Hz\r\n", DS3231_I2C_CLOCK_HZ, DS3231_I2C_BAUDRATE);

    return true;
}

bool DS3231_TestCommunication(void)
{
    uint8_t secondsRaw = 0U;
    uint8_t seconds;

    PRINTF("[RTC] Probing DS3231 at address 0x68...\r\n");

    if (!DS3231_ReadRegisters(DS3231_REGISTER_SECONDS, &secondsRaw, 1U))
    {
        PRINTF("[RTC] ERROR: DS3231 not detected.\r\n");
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

bool DS3231_ReadDateTime(ds3231_datetime_t *dateTime)
{
    uint8_t registers[7];
    uint8_t hourRaw;
    uint8_t monthRaw;

    if (dateTime == NULL)
    {
        return false;
    }

    if (!DS3231_ReadRegisters(DS3231_REGISTER_SECONDS, registers, sizeof(registers)))
    {
        return false;
    }

    dateTime->second = DS3231_BcdToDecimal(registers[0] & 0x7FU);
    dateTime->minute = DS3231_BcdToDecimal(registers[1] & 0x7FU);

    hourRaw = registers[2];

    if ((hourRaw & 0x40U) != 0U)
    {
        uint8_t hour12 = DS3231_BcdToDecimal(hourRaw & 0x1FU);
        bool isPm = (hourRaw & 0x20U) != 0U;

        if (hour12 == 12U)
        {
            dateTime->hour = isPm ? 12U : 0U;
        }
        else
        {
            dateTime->hour = isPm ? (uint8_t)(hour12 + 12U) : hour12;
        }
    }
    else
    {
        dateTime->hour = DS3231_BcdToDecimal(hourRaw & 0x3FU);
    }

    dateTime->day = DS3231_BcdToDecimal(registers[3] & 0x07U);
    dateTime->date = DS3231_BcdToDecimal(registers[4] & 0x3FU);

    monthRaw = registers[5];
    dateTime->month = DS3231_BcdToDecimal(monthRaw & 0x1FU);

    dateTime->year = (uint16_t)(2000U + DS3231_BcdToDecimal(registers[6]));

    if ((monthRaw & 0x80U) != 0U)
    {
        dateTime->year += 100U;
    }

    if (!DS3231_IsDateTimeRangeValid(dateTime))
    {
        PRINTF("[RTC] ERROR: Date/time register contains invalid values.\r\n");
        return false;
    }

    return true;
}

bool DS3231_SetDateTime(const ds3231_datetime_t *dateTime)
{
    uint8_t registers[7];

    if (!DS3231_IsDateTimeRangeValid(dateTime))
    {
        PRINTF("[RTC] ERROR: Refusing to write invalid date/time.\r\n");
        return false;
    }

    if (dateTime->year > 2099U)
    {
        PRINTF("[RTC] ERROR: Current implementation supports year 2000-2099.\r\n");
        return false;
    }

    registers[0] = DS3231_DecimalToBcd(dateTime->second);
    registers[1] = DS3231_DecimalToBcd(dateTime->minute);
    registers[2] = DS3231_DecimalToBcd(dateTime->hour);
    registers[3] = DS3231_DecimalToBcd(dateTime->day);
    registers[4] = DS3231_DecimalToBcd(dateTime->date);
    registers[5] = DS3231_DecimalToBcd(dateTime->month);
    registers[6] = DS3231_DecimalToBcd((uint8_t)(dateTime->year - 2000U));

    if (!DS3231_WriteRegisters(DS3231_REGISTER_SECONDS, registers, sizeof(registers)))
    {
        PRINTF("[RTC] ERROR: Unable to write date/time.\r\n");
        return false;
    }

    if (!DS3231_ClearOscillatorStopFlag())
    {
        PRINTF("[RTC] WARNING: Date/time written but OSF could not be cleared.\r\n");
    }

    PRINTF("[RTC] Date/time written successfully.\r\n");

    return true;
}

bool DS3231_IsTimeValid(void)
{
    uint8_t statusRegister = 0U;

    if (!DS3231_ReadRegisters(DS3231_REGISTER_STATUS, &statusRegister, 1U))
    {
        return false;
    }

    if ((statusRegister & DS3231_STATUS_OSF_MASK) != 0U)
    {
        PRINTF("[RTC] Time invalid | oscillator stop flag is set.\r\n");
        return false;
    }

    return true;
}

bool DS3231_ClearOscillatorStopFlag(void)
{
    uint8_t statusRegister = 0U;

    if (!DS3231_ReadRegisters(DS3231_REGISTER_STATUS, &statusRegister, 1U))
    {
        return false;
    }

    statusRegister &= (uint8_t)(~DS3231_STATUS_OSF_MASK);

    if (!DS3231_WriteRegisters(DS3231_REGISTER_STATUS, &statusRegister, 1U))
    {
        return false;
    }

    return true;
}

void DS3231_PrintDateTime(const ds3231_datetime_t *dateTime)
{
    if (dateTime == NULL)
    {
        return;
    }

    PRINTF(
        "[RTC] Date/time = %04u-%02u-%02u %02u:%02u:%02u | day=%u\r\n",
        dateTime->year,
        dateTime->month,
        dateTime->date,
        dateTime->hour,
        dateTime->minute,
        dateTime->second,
        dateTime->day
    );
}