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

bool DS3231_ReadDateTime(ds3231_datetime_t *dateTime)
{
    uint8_t secondsRaw = 0U;
    uint8_t minutesRaw = 0U;
    uint8_t hoursRaw = 0U;
    uint8_t dayRaw = 0U;
    uint8_t dateRaw = 0U;
    uint8_t monthRaw = 0U;
    uint8_t yearRaw = 0U;

    if (dateTime == NULL)
    {
        return false;
    }

    /*
     * Penting:
     *
     * Setiap register dibaca menggunakan transaksi I2C terpisah
     * dengan dataSize = 1.
     *
     * Ini mengikuti pola yang sudah terbukti stabil pada board.
     */

    PRINTF("[RTC] Reading seconds register 0x00...\r\n");

    if (!DS3231_ReadRegister(DS3231_REGISTER_SECONDS, &secondsRaw))
    {
        return false;
    }

    PRINTF(
        "[RTC] Seconds | raw=0x%02X | decoded=%u\r\n",
        secondsRaw,
        DS3231_BcdToDecimal(secondsRaw & 0x7FU)
    );

    PRINTF("[RTC] Reading minutes register 0x01...\r\n");

    if (!DS3231_ReadRegister(DS3231_REGISTER_MINUTES, &minutesRaw))
    {
        return false;
    }

    PRINTF(
        "[RTC] Minutes | raw=0x%02X | decoded=%u\r\n",
        minutesRaw,
        DS3231_BcdToDecimal(minutesRaw & 0x7FU)
    );

    PRINTF("[RTC] Reading hours register 0x02...\r\n");

    if (!DS3231_ReadRegister(DS3231_REGISTER_HOURS, &hoursRaw))
    {
        return false;
    }

    PRINTF(
        "[RTC] Hours | raw=0x%02X\r\n",
        hoursRaw
    );

    PRINTF("[RTC] Reading day register 0x03...\r\n");

    if (!DS3231_ReadRegister(DS3231_REGISTER_DAY, &dayRaw))
    {
        return false;
    }

    PRINTF(
        "[RTC] Day | raw=0x%02X | decoded=%u\r\n",
        dayRaw,
        DS3231_BcdToDecimal(dayRaw & 0x07U)
    );

    PRINTF("[RTC] Reading date register 0x04...\r\n");

    if (!DS3231_ReadRegister(DS3231_REGISTER_DATE, &dateRaw))
    {
        return false;
    }

    PRINTF(
        "[RTC] Date | raw=0x%02X | decoded=%u\r\n",
        dateRaw,
        DS3231_BcdToDecimal(dateRaw & 0x3FU)
    );

    PRINTF("[RTC] Reading month register 0x05...\r\n");

    if (!DS3231_ReadRegister(DS3231_REGISTER_MONTH, &monthRaw))
    {
        return false;
    }

    PRINTF(
        "[RTC] Month | raw=0x%02X | decoded=%u\r\n",
        monthRaw,
        DS3231_BcdToDecimal(monthRaw & 0x1FU)
    );

    PRINTF("[RTC] Reading year register 0x06...\r\n");

    if (!DS3231_ReadRegister(DS3231_REGISTER_YEAR, &yearRaw))
    {
        return false;
    }

    PRINTF(
        "[RTC] Year | raw=0x%02X | decoded=%u\r\n",
        yearRaw,
        DS3231_BcdToDecimal(yearRaw)
    );

    /*
     * Seconds.
     */
    dateTime->second = DS3231_BcdToDecimal(secondsRaw & 0x7FU);

    /*
     * Minutes.
     */
    dateTime->minute = DS3231_BcdToDecimal(minutesRaw & 0x7FU);

    /*
     * Hours.
     *
     * Bit 6:
     * 0 = mode 24 jam
     * 1 = mode 12 jam
     */
    if ((hoursRaw & 0x40U) != 0U)
    {
        uint8_t hour12 = DS3231_BcdToDecimal(hoursRaw & 0x1FU);
        bool isPm = (hoursRaw & 0x20U) != 0U;

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
        dateTime->hour = DS3231_BcdToDecimal(hoursRaw & 0x3FU);
    }

    /*
     * Day of week.
     */
    dateTime->day = DS3231_BcdToDecimal(dayRaw & 0x07U);

    /*
     * Date.
     */
    dateTime->date = DS3231_BcdToDecimal(dateRaw & 0x3FU);

    /*
     * Month.
     *
     * Bit 7 merupakan century bit.
     */
    dateTime->month = DS3231_BcdToDecimal(monthRaw & 0x1FU);

    /*
     * Year.
     */
    dateTime->year = (uint16_t)(2000U + DS3231_BcdToDecimal(yearRaw));

    if ((monthRaw & 0x80U) != 0U)
    {
        dateTime->year += 100U;
    }

    PRINTF("[RTC] Complete date/time register read successful.\r\n");

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