/*
 * Copyright (c) 2015, Freescale Semiconductor, Inc.
 * Copyright 2016-2017, 2024 NXP
 * All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

/* FreeRTOS kernel includes. */
#include "FreeRTOS.h"
#include "task.h"
#include "queue.h"
#include "timers.h"

/* Freescale includes. */
#include "fsl_device_registers.h"
#include "fsl_debug_console.h"
#include "pin_mux.h"
#include "clock_config.h"
#include "board.h"
#include "LLMJVM.h"
#include "LLBSP_impl.h"
#include "sni.h"

#include "fsl_inputmux.h"
#include "fsl_dma.h"
#include "fsl_gpio.h"

#include "fsl_io_mux.h"
#include "fsl_spi.h"

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#include "pin_mux.h"
#include "clock_config.h"
#include "board.h"
#include "fsl_lcdic.h"
#include "fsl_lcdic_dma.h"
#include "fsl_debug_console.h"
#include "panel_func.h"

#include "fsl_inputmux.h"
#include "fsl_reset.h"
#include "display_support.h"

#include "cpuload.h"

#include <assert.h>
#include "els_pkc_mbedtls.h"

#include "tree_version.h"

#ifdef ENABLE_SYSTEM_VIEW
#include "SEGGER_SYSVIEW.h"
#include "SEGGER_RTT.h"
#include "npavee.h"
#endif

#ifdef ENABLE_WIFI
#include "fsl_power.h"
#endif

#include "shell.h"

/*******************************************************************************
 * Definitions
 ******************************************************************************/

/* Task priorities. */
#define nxp_pa_task_PRIORITY (configMAX_PRIORITIES - 6)

/*
 * LoRa smoke test configuration.
 */
#define lora_test_task_PRIORITY (tskIDLE_PRIORITY + 1U)
#define LORA_TEST_TASK_STACK_SIZE 768U

/*
 * RFM95W menggunakan FLEXCOMM1 / SPI1.
 */
#define LORA_SPI SPI1
#define LORA_SPI_CLOCK_INDEX 1U
#define LORA_SPI_BAUDRATE_BPS 1000000U

/*
 * Arduino D9 pada FRDM-RW612 adalah GPIO52.
 * GPIO52 = port 1, pin 20.
 */
#define LORA_RESET_GPIO_PORT 1U
#define LORA_RESET_GPIO_PIN 20U

/*
 * Register identitas RFM95W.
 */
#define RFM95_REG_VERSION 0x42U
#define RFM95_EXPECTED_VERSION 0x12U

/*
 * Ukuran payload sensor terbaru:
 *
 * 2 x uint8_t  = 2 byte
 * 10 x int16_t = 20 byte
 * Total        = 22 byte
 */
#define LORA_SENSOR_PAYLOAD_LENGTH 22U

/*
 * Format snapshot yang dikirim dari native ke Java.
 *
 * Posisi index harus sama dengan konstanta
 * yang terdapat pada LoRaNative.java.
 */
#define LORA_JAVA_SNAPSHOT_LENGTH              15U

#define LORA_SNAPSHOT_SEQUENCE_INDEX           0U
#define LORA_SNAPSHOT_NODE_ID_INDEX            1U
#define LORA_SNAPSHOT_NODE_TARGET_INDEX        2U
#define LORA_SNAPSHOT_SOIL_MOISTURE_INDEX      3U
#define LORA_SNAPSHOT_SOIL_TEMPERATURE_INDEX   4U
#define LORA_SNAPSHOT_CONDUCTIVITY_INDEX       5U
#define LORA_SNAPSHOT_SOIL_PH_INDEX            6U
#define LORA_SNAPSHOT_NITROGEN_INDEX           7U
#define LORA_SNAPSHOT_PHOSPHORUS_INDEX         8U
#define LORA_SNAPSHOT_POTASSIUM_INDEX          9U
#define LORA_SNAPSHOT_AIR_TEMPERATURE_INDEX    10U
#define LORA_SNAPSHOT_AIR_HUMIDITY_INDEX       11U
#define LORA_SNAPSHOT_LIGHT_INTENSITY_INDEX    12U
#define LORA_SNAPSHOT_RSSI_INDEX               13U
#define LORA_SNAPSHOT_SNR_INDEX                14U

/*
 * Struktur penyimpanan data LoRa terbaru di native.
 *
 * Ini bukan struct yang dikirim langsung melalui radio.
 * Struct ini hanya dipakai untuk menyimpan hasil parsing.
 */
typedef struct
{
    uint8_t node_id;
    uint8_t node_target;

    int16_t soil_moisture;
    int16_t soil_temperature;
    int16_t conductivity;
    int16_t soil_ph;
    int16_t nitrogen;
    int16_t phosphorus;
    int16_t potassium;
    int16_t air_temperature;
    int16_t air_humidity;
    int16_t light_intensity;

    int32_t rssi;
    int32_t snr_quarter_db;

    uint32_t sequence;
    bool available;
} lora_latest_data_t;

/*
 * Konfigurasi radio berdasarkan kode lama node sensor.
 *
 * Jika versi terbaru teman menggunakan 915 MHz,
 * cukup ubah menjadi 915000000UL.
 */
#define RFM95_FREQUENCY_HZ 923000000UL
#define RFM95_FXOSC_HZ 32000000UL
#define RFM95_MAX_PACKET_LENGTH 255U

/*
 * Register SX1276/RFM95W yang diperlukan
 * untuk menerima paket LoRa.
 */
#define RFM95_REG_FIFO 0x00U
#define RFM95_REG_OP_MODE 0x01U

#define RFM95_REG_FRF_MSB 0x06U
#define RFM95_REG_FRF_MID 0x07U
#define RFM95_REG_FRF_LSB 0x08U

#define RFM95_REG_LNA 0x0CU

#define RFM95_REG_FIFO_ADDR_PTR 0x0DU
#define RFM95_REG_FIFO_TX_BASE_ADDR 0x0EU
#define RFM95_REG_FIFO_RX_BASE_ADDR 0x0FU
#define RFM95_REG_FIFO_RX_CURRENT_ADDR 0x10U

#define RFM95_REG_IRQ_FLAGS 0x12U
#define RFM95_REG_RX_NB_BYTES 0x13U

#define RFM95_REG_PKT_SNR_VALUE 0x19U
#define RFM95_REG_PKT_RSSI_VALUE 0x1AU

#define RFM95_REG_MODEM_CONFIG_1 0x1DU
#define RFM95_REG_MODEM_CONFIG_2 0x1EU

#define RFM95_REG_PREAMBLE_MSB 0x20U
#define RFM95_REG_PREAMBLE_LSB 0x21U
#define RFM95_REG_MAX_PAYLOAD_LENGTH 0x23U
#define RFM95_REG_MODEM_CONFIG_3 0x26U

#define RFM95_REG_DETECTION_OPTIMIZE 0x31U
#define RFM95_REG_INVERT_IQ 0x33U
#define RFM95_REG_DETECTION_THRESHOLD 0x37U
#define RFM95_REG_SYNC_WORD 0x39U
#define RFM95_REG_INVERT_IQ_2 0x3BU
#define RFM95_REG_DIO_MAPPING_1 0x40U

/*
 * Operating mode.
 */
#define RFM95_MODE_LONG_RANGE 0x80U
#define RFM95_MODE_SLEEP 0x00U
#define RFM95_MODE_STANDBY 0x01U
#define RFM95_MODE_RX_CONTINUOUS 0x05U

/*
 * IRQ flags.
 */
#define RFM95_IRQ_PAYLOAD_CRC_ERROR 0x20U
#define RFM95_IRQ_RX_DONE 0x40U

typedef struct
{
    uint8_t node_id;
    uint8_t node_target;

    int16_t soil_moisture;
    int16_t soil_temperature;
    int16_t conductivity;
    int16_t soil_ph;
    int16_t nitrogen;
    int16_t phosphorus;
    int16_t potassium;
    int16_t air_temperature;
    int16_t air_humidity;
    int16_t light_intensity;
} lora_sensor_payload_t;

_Static_assert(
    sizeof(lora_sensor_payload_t) == 22U,
    "LoRa sensor payload must be exactly 22 bytes");

/*******************************************************************************
 * Prototypes
 ******************************************************************************/
static void nxp_pa_task(void *pvParameters);

static void lora_test_task(void *pvParameters);

static void LORA_InitSpi(void);

static void LORA_Reset(void);

static bool LORA_ReadRegister(
    uint8_t address,
    uint8_t *value);

static bool LORA_WriteRegister(
    uint8_t address,
    uint8_t value);

static bool LORA_ConfigureReceiver(void);

static int32_t LORA_ReceivePacket(
    uint8_t *buffer,
    uint32_t capacity,
    int32_t *rssi,
    int32_t *snrQuarterDb);

static void LORA_PrintPacket(
    const uint8_t *buffer,
    uint32_t length,
    int32_t rssi,
    int32_t snrQuarterDb);

static int16_t LORA_ReadInt16LE(
    const uint8_t *buffer,
    uint32_t offset);

static void LORA_PrintSensorPayload(
    const uint8_t *buffer,
    uint32_t length);

static bool LORA_SaveLatestPayload(
    const uint8_t *buffer,
    uint32_t length,
    int32_t rssi,
    int32_t snrQuarterDb);

static status_t LORA_SpiTransfer(
    uint8_t *txData,
    uint8_t *rxData,
    size_t dataSize);

static void BOARD_InitLcdicClock();

/*******************************************************************************
 *  Global variables
 ******************************************************************************/
TaskHandle_t pvMicrojvmCreatedTask = NULL;

/*
 * Menyimpan satu paket sensor terbaru.
 *
 * Task LoRa menulis data ini.
 * Native method Java nantinya membaca data ini.
 */
static lora_latest_data_t g_loraLatestData = {0};

/*******************************************************************************
 * Code
 ******************************************************************************/
/**
 * Menjalankan transfer SPI blocking.
 *
 * CS/SSEL0 akan aktif selama seluruh transfer
 * dan dilepas setelah transfer selesai.
 */
static status_t LORA_SpiTransfer(
    uint8_t *txData,
    uint8_t *rxData,
    size_t dataSize)
{
    spi_transfer_t transfer = {0};

    transfer.txData = txData;
    transfer.rxData = rxData;
    transfer.dataSize = dataSize;
    transfer.configFlags = kSPI_FrameAssert;

    return SPI_MasterTransferBlocking(
        LORA_SPI,
        &transfer);
}

/**
 * Membaca satu register RFM95W.
 *
 * Bit 7 address harus 0 untuk operasi read.
 */
static bool LORA_ReadRegister(
    uint8_t address,
    uint8_t *value)
{
    uint8_t txData[2];
    uint8_t rxData[2];

    if (value == NULL)
    {
        return false;
    }

    txData[0] = address & 0x7FU;
    txData[1] = 0x00U;

    rxData[0] = 0x00U;
    rxData[1] = 0x00U;

    status_t status =
        LORA_SpiTransfer(
            txData,
            rxData,
            sizeof(txData));

    if (status != kStatus_Success)
    {
        return false;
    }

    /*
     * Byte pertama adalah hasil selama address dikirim.
     * Nilai register berada pada byte kedua.
     */
    *value = rxData[1];

    return true;
}

/**
 * Menulis satu byte ke register RFM95W.
 *
 * Bit 7 address harus bernilai 1 untuk operasi write.
 */
static bool LORA_WriteRegister(
    uint8_t address,
    uint8_t value)
{
    uint8_t txData[2];
    uint8_t rxData[2];

    status_t status;

    txData[0] =
        address | 0x80U;

    txData[1] =
        value;

    rxData[0] = 0x00U;
    rxData[1] = 0x00U;

    status =
        LORA_SpiTransfer(
            txData,
            rxData,
            sizeof(txData));

    return status == kStatus_Success;
}

/**
 * Mengatur Arduino D9 / GPIO52 sebagai reset RFM95W.
 */
static void LORA_Reset(void)
{
    gpio_pin_config_t resetConfiguration = {
        kGPIO_DigitalOutput,
        1U};

    /*
     * GPIO52 digunakan sebagai GPIO biasa,
     * bukan peripheral alternatif.
     */
    IO_MUX_SetPinMux(
        IO_MUX_GPIO52);

    GPIO_PinInit(
        GPIO,
        LORA_RESET_GPIO_PORT,
        LORA_RESET_GPIO_PIN,
        &resetConfiguration);

    /*
     * Kondisi awal reset tidak aktif.
     */
    GPIO_PinWrite(
        GPIO,
        LORA_RESET_GPIO_PORT,
        LORA_RESET_GPIO_PIN,
        1U);

    vTaskDelay(
        pdMS_TO_TICKS(2U));

    /*
     * Reset aktif rendah.
     */
    GPIO_PinWrite(
        GPIO,
        LORA_RESET_GPIO_PORT,
        LORA_RESET_GPIO_PIN,
        0U);

    vTaskDelay(
        pdMS_TO_TICKS(10U));

    /*
     * Lepaskan reset.
     */
    GPIO_PinWrite(
        GPIO,
        LORA_RESET_GPIO_PORT,
        LORA_RESET_GPIO_PIN,
        1U);

    vTaskDelay(
        pdMS_TO_TICKS(10U));
}

/**
 * Menginisialisasi FLEXCOMM1 sebagai SPI master.
 */
static void LORA_InitSpi(void)
{
    spi_master_config_t masterConfiguration;

    /*
     * Gunakan SFRO sebagai clock FLEXCOMM1.
     */
    CLOCK_AttachClk(
        kSFRO_to_FLEXCOMM1);

    /*
     * Reset peripheral FLEXCOMM1.
     */
    RESET_PeripheralReset(
        kFC1_RST_SHIFT_RSTn);

    /*
     * Mengaktifkan pin:
     *
     * GPIO6 = SPI1 SSEL0 / CS
     * GPIO7 = SPI1 SCK
     * GPIO8 = SPI1 MISO
     * GPIO9 = SPI1 MOSI
     */
    IO_MUX_SetPinMux(
        IO_MUX_FC1_SPI_SS0);

    SPI_MasterGetDefaultConfig(
        &masterConfiguration);

    /*
     * RFM95W menggunakan:
     *
     * SPI mode 0
     * clock idle low
     * sample pada first edge
     * MSB first
     * SSEL0 aktif rendah
     */
    masterConfiguration.baudRate_Bps =
        LORA_SPI_BAUDRATE_BPS;

    masterConfiguration.polarity =
        kSPI_ClockPolarityActiveHigh;

    masterConfiguration.phase =
        kSPI_ClockPhaseFirstEdge;

    masterConfiguration.direction =
        kSPI_MsbFirst;

    masterConfiguration.sselNum =
        kSPI_Ssel0;

    SPI_MasterInit(
        LORA_SPI,
        &masterConfiguration,
        CLOCK_GetFlexCommClkFreq(
            LORA_SPI_CLOCK_INDEX));
}

/**
 * Mengonfigurasi RFM95W agar kompatibel
 * dengan node sensor Arduino.
 */
static bool LORA_ConfigureReceiver(void)
{
    uint64_t frequencyRegister;
    uint8_t lnaValue;

    /*
     * Masuk ke LoRa sleep mode.
     * LongRangeMode hanya boleh diubah ketika sleep.
     */
    if (!LORA_WriteRegister(
            RFM95_REG_OP_MODE,
            RFM95_MODE_LONG_RANGE | RFM95_MODE_SLEEP))
    {
        return false;
    }

    vTaskDelay(
        pdMS_TO_TICKS(10U));

    /*
     * FRF = frequency × 2^19 / 32 MHz.
     *
     * Untuk 923 MHz hasilnya 0xE6C000.
     */
    frequencyRegister =
        (((uint64_t)RFM95_FREQUENCY_HZ) << 19U) / RFM95_FXOSC_HZ;

    if (!LORA_WriteRegister(
            RFM95_REG_FRF_MSB,
            (uint8_t)(frequencyRegister >> 16U)))
    {
        return false;
    }

    if (!LORA_WriteRegister(
            RFM95_REG_FRF_MID,
            (uint8_t)(frequencyRegister >> 8U)))
    {
        return false;
    }

    if (!LORA_WriteRegister(
            RFM95_REG_FRF_LSB,
            (uint8_t)frequencyRegister))
    {
        return false;
    }

    /*
     * FIFO TX dan RX dimulai dari alamat nol.
     */
    if (!LORA_WriteRegister(
            RFM95_REG_FIFO_TX_BASE_ADDR,
            0x00U))
    {
        return false;
    }

    if (!LORA_WriteRegister(
            RFM95_REG_FIFO_RX_BASE_ADDR,
            0x00U))
    {
        return false;
    }

    if (!LORA_WriteRegister(
            RFM95_REG_FIFO_ADDR_PTR,
            0x00U))
    {
        return false;
    }

    /*
     * RegModemConfig1:
     *
     * 0x70 = bandwidth 125 kHz
     * 0x02 = coding rate 4/5
     * 0x00 = explicit header
     *
     * Total = 0x72.
     */
    if (!LORA_WriteRegister(
            RFM95_REG_MODEM_CONFIG_1,
            0x72U))
    {
        return false;
    }

    /*
     * RegModemConfig2:
     *
     * 0x70 = spreading factor 7
     * CRC payload tetap nonaktif.
     */
    if (!LORA_WriteRegister(
            RFM95_REG_MODEM_CONFIG_2,
            0x70U))
    {
        return false;
    }

    /*
     * AGC otomatis aktif.
     * Low Data Rate Optimization tidak diperlukan
     * untuk SF7 dan bandwidth 125 kHz.
     */
    if (!LORA_WriteRegister(
            RFM95_REG_MODEM_CONFIG_3,
            0x04U))
    {
        return false;
    }

    /*
     * Nilai detection untuk SF7-SF12.
     */
    if (!LORA_WriteRegister(
            RFM95_REG_DETECTION_OPTIMIZE,
            0xC3U))
    {
        return false;
    }

    if (!LORA_WriteRegister(
            RFM95_REG_DETECTION_THRESHOLD,
            0x0AU))
    {
        return false;
    }

    /*
     * Preamble default Arduino LoRa = 8 symbols.
     */
    if (!LORA_WriteRegister(
            RFM95_REG_PREAMBLE_MSB,
            0x00U))
    {
        return false;
    }

    if (!LORA_WriteRegister(
            RFM95_REG_PREAMBLE_LSB,
            0x08U))
    {
        return false;
    }

    /*
     * Sync word default Arduino LoRa.
     */
    if (!LORA_WriteRegister(
            RFM95_REG_SYNC_WORD,
            0x12U))
    {
        return false;
    }

    /*
     * Normal IQ, bukan inverted IQ.
     */
    if (!LORA_WriteRegister(
            RFM95_REG_INVERT_IQ,
            0x27U))
    {
        return false;
    }

    if (!LORA_WriteRegister(
            RFM95_REG_INVERT_IQ_2,
            0x1DU))
    {
        return false;
    }

    /*
     * DIO0 diarahkan ke RxDone.
     *
     * DIO0 belum dipakai oleh kode polling ini,
     * tetapi mapping dibuat sama dengan mode receive
     * pada library Arduino.
     */
    if (!LORA_WriteRegister(
            RFM95_REG_DIO_MAPPING_1,
            0x00U))
    {
        return false;
    }

    if (!LORA_WriteRegister(
            RFM95_REG_MAX_PAYLOAD_LENGTH,
            RFM95_MAX_PACKET_LENGTH))
    {
        return false;
    }

    /*
     * Aktifkan LNA boost.
     */
    if (!LORA_ReadRegister(
            RFM95_REG_LNA,
            &lnaValue))
    {
        return false;
    }

    if (!LORA_WriteRegister(
            RFM95_REG_LNA,
            lnaValue | 0x03U))
    {
        return false;
    }

    /*
     * Bersihkan seluruh interrupt lama.
     */
    if (!LORA_WriteRegister(
            RFM95_REG_IRQ_FLAGS,
            0xFFU))
    {
        return false;
    }

    /*
     * Masuk standby sebelum RX continuous.
     */
    if (!LORA_WriteRegister(
            RFM95_REG_OP_MODE,
            RFM95_MODE_LONG_RANGE | RFM95_MODE_STANDBY))
    {
        return false;
    }

    vTaskDelay(
        pdMS_TO_TICKS(10U));

    /*
     * Receiver aktif terus-menerus.
     */
    if (!LORA_WriteRegister(
            RFM95_REG_OP_MODE,
            RFM95_MODE_LONG_RANGE | RFM95_MODE_RX_CONTINUOUS))
    {
        return false;
    }

    return true;
}

/**
 * Membaca satu paket LoRa apabila tersedia.
 *
 * Return:
 *   > 0 : panjang paket
 *     0 : belum ada paket
 *    -1 : paket mengalami CRC error
 *    -2 : operasi SPI/register gagal
 */
static int32_t LORA_ReceivePacket(
    uint8_t *buffer,
    uint32_t capacity,
    int32_t *rssi,
    int32_t *snrQuarterDb)
{
    uint8_t irqFlags;
    uint8_t packetLength;
    uint8_t currentFifoAddress;
    uint8_t rawRssi;
    uint8_t rawSnr;

    uint32_t index;

    if (buffer == NULL || capacity == 0U || rssi == NULL || snrQuarterDb == NULL)
    {

        return -2;
    }

    if (!LORA_ReadRegister(
            RFM95_REG_IRQ_FLAGS,
            &irqFlags))
    {
        return -2;
    }

    /*
     * Belum ada paket selesai diterima.
     */
    if ((irqFlags & RFM95_IRQ_RX_DONE) == 0U)
    {
        return 0;
    }

    /*
     * Menulis kembali bit IRQ akan membersihkannya.
     */
    if (!LORA_WriteRegister(
            RFM95_REG_IRQ_FLAGS,
            irqFlags))
    {
        return -2;
    }

    /*
     * Paket dengan CRC error dibuang.
     *
     * Jika transmitter tidak menggunakan CRC,
     * flag ini normalnya tidak aktif.
     */
    if ((irqFlags & RFM95_IRQ_PAYLOAD_CRC_ERROR) != 0U)
    {

        return -1;
    }

    /*
     * Explicit-header mode memakai RegRxNbBytes
     * sebagai panjang paket.
     */
    if (!LORA_ReadRegister(
            RFM95_REG_RX_NB_BYTES,
            &packetLength))
    {
        return -2;
    }

    if (!LORA_ReadRegister(
            RFM95_REG_FIFO_RX_CURRENT_ADDR,
            &currentFifoAddress))
    {
        return -2;
    }

    if (!LORA_WriteRegister(
            RFM95_REG_FIFO_ADDR_PTR,
            currentFifoAddress))
    {
        return -2;
    }

    if ((uint32_t)packetLength > capacity)
    {
        packetLength =
            (uint8_t)capacity;
    }

    /*
     * Membaca byte FIFO satu per satu.
     *
     * Setiap pembacaan FIFO otomatis memajukan
     * pointer FIFO internal.
     */
    for (index = 0U;
         index < (uint32_t)packetLength;
         index++)
    {

        if (!LORA_ReadRegister(
                RFM95_REG_FIFO,
                &buffer[index]))
        {
            return -2;
        }
    }

    if (!LORA_ReadRegister(
            RFM95_REG_PKT_RSSI_VALUE,
            &rawRssi))
    {
        return -2;
    }

    if (!LORA_ReadRegister(
            RFM95_REG_PKT_SNR_VALUE,
            &rawSnr))
    {
        return -2;
    }

    /*
     * Pada frekuensi HF seperti 923 MHz,
     * Arduino LoRa menggunakan offset RSSI 157.
     */
    *rssi =
        ((int32_t)rawRssi) - 157;

    /*
     * Register SNR bertipe signed dan mempunyai
     * satuan 0,25 dB.
     */
    *snrQuarterDb =
        (int32_t)((int8_t)rawSnr);

    return (int32_t)packetLength;
}

static int16_t LORA_ReadInt16LE(
    const uint8_t *buffer,
    uint32_t offset)
{
    uint16_t value;

    value =
        ((uint16_t)buffer[offset]) | ((uint16_t)buffer[offset + 1U] << 8U);

    return (int16_t)value;
}

/**
 * Menyimpan payload sensor terbaru ke memori native.
 *
 * Data mentah tetap disimpan sebagai integer.
 * Pembagian dengan 10 dilakukan nanti di Java.
 */
static bool LORA_SaveLatestPayload(
    const uint8_t *buffer,
    uint32_t length,
    int32_t rssi,
    int32_t snrQuarterDb)
{
    if (buffer == NULL || length != LORA_SENSOR_PAYLOAD_LENGTH)
    {

        return false;
    }

    /*
     * Task LoRa menulis data, sementara Java nantinya
     * akan membacanya dari task MicroJVM.
     *
     * Critical section mencegah Java membaca ketika
     * separuh field masih dalam proses diperbarui.
     */
    taskENTER_CRITICAL();

    g_loraLatestData.node_id =
        buffer[0];

    g_loraLatestData.node_target =
        buffer[1];

    g_loraLatestData.soil_moisture =
        LORA_ReadInt16LE(buffer, 2U);

    g_loraLatestData.soil_temperature =
        LORA_ReadInt16LE(buffer, 4U);

    g_loraLatestData.conductivity =
        LORA_ReadInt16LE(buffer, 6U);

    g_loraLatestData.soil_ph =
        LORA_ReadInt16LE(buffer, 8U);

    g_loraLatestData.nitrogen =
        LORA_ReadInt16LE(buffer, 10U);

    g_loraLatestData.phosphorus =
        LORA_ReadInt16LE(buffer, 12U);

    g_loraLatestData.potassium =
        LORA_ReadInt16LE(buffer, 14U);

    g_loraLatestData.air_temperature =
        LORA_ReadInt16LE(buffer, 16U);

    g_loraLatestData.air_humidity =
        LORA_ReadInt16LE(buffer, 18U);

    g_loraLatestData.light_intensity =
        LORA_ReadInt16LE(buffer, 20U);

    g_loraLatestData.rssi =
        rssi;

    g_loraLatestData.snr_quarter_db =
        snrQuarterDb;

    /*
     * Setiap paket valid menaikkan sequence.
     */
    g_loraLatestData.sequence++;

    g_loraLatestData.available =
        true;

    taskEXIT_CRITICAL();

    return true;
}

/**
 * Native implementation untuk:
 *
 * LoRaNative.readLatestNative(int[] destination)
 *
 * Fungsi ini tidak boleh diberi keyword static karena
 * simbolnya harus terlihat oleh linker MicroEJ SNI.
 */
jboolean
Java_com_nxp_example_smartgreenhouse_services_lora_LoRaNative_readLatestNative(
        jint *destination
) {
    lora_latest_data_t snapshot;
    jint destinationLength;

    /*
     * Pemeriksaan defensif. Java wrapper seharusnya
     * sudah mencegah array null.
     */
    if (destination == NULL) {
        return JFALSE;
    }

    destinationLength =
            SNI_getArrayLength(destination);

    if (destinationLength
            < (jint)LORA_JAVA_SNAPSHOT_LENGTH) {

        return JFALSE;
    }

    /*
     * Ambil salinan satu paket secara atomik.
     *
     * Task LoRa dapat memperbarui g_loraLatestData,
     * sedangkan native method ini dipanggil oleh
     * task MicroJVM.
     */
    taskENTER_CRITICAL();

    if (!g_loraLatestData.available) {
        taskEXIT_CRITICAL();

        return JFALSE;
    }

    snapshot =
            g_loraLatestData;

    taskEXIT_CRITICAL();

    /*
     * Setelah critical section selesai, snapshot lokal
     * tidak akan berubah meskipun paket LoRa baru masuk.
     */
    destination[
            LORA_SNAPSHOT_SEQUENCE_INDEX
    ] = (jint)snapshot.sequence;

    destination[
            LORA_SNAPSHOT_NODE_ID_INDEX
    ] = (jint)snapshot.node_id;

    destination[
            LORA_SNAPSHOT_NODE_TARGET_INDEX
    ] = (jint)snapshot.node_target;

    destination[
            LORA_SNAPSHOT_SOIL_MOISTURE_INDEX
    ] = (jint)snapshot.soil_moisture;

    destination[
            LORA_SNAPSHOT_SOIL_TEMPERATURE_INDEX
    ] = (jint)snapshot.soil_temperature;

    destination[
            LORA_SNAPSHOT_CONDUCTIVITY_INDEX
    ] = (jint)snapshot.conductivity;

    destination[
            LORA_SNAPSHOT_SOIL_PH_INDEX
    ] = (jint)snapshot.soil_ph;

    destination[
            LORA_SNAPSHOT_NITROGEN_INDEX
    ] = (jint)snapshot.nitrogen;

    destination[
            LORA_SNAPSHOT_PHOSPHORUS_INDEX
    ] = (jint)snapshot.phosphorus;

    destination[
            LORA_SNAPSHOT_POTASSIUM_INDEX
    ] = (jint)snapshot.potassium;

    destination[
            LORA_SNAPSHOT_AIR_TEMPERATURE_INDEX
    ] = (jint)snapshot.air_temperature;

    destination[
            LORA_SNAPSHOT_AIR_HUMIDITY_INDEX
    ] = (jint)snapshot.air_humidity;

    destination[
            LORA_SNAPSHOT_LIGHT_INTENSITY_INDEX
    ] = (jint)snapshot.light_intensity;

    destination[
            LORA_SNAPSHOT_RSSI_INDEX
    ] = (jint)snapshot.rssi;

    destination[
            LORA_SNAPSHOT_SNR_INDEX
    ] = (jint)snapshot.snr_quarter_db;

    return JTRUE;
}

/**
 * Menampilkan paket dalam bentuk HEX dan ASCII.
 */
static void LORA_PrintPacket(
    const uint8_t *buffer,
    uint32_t length,
    int32_t rssi,
    int32_t snrQuarterDb)
{
    uint32_t index;

    int32_t absoluteSnr;
    int32_t snrWhole;
    int32_t snrFraction;

    const char *snrSign;

    absoluteSnr =
        snrQuarterDb < 0
            ? -snrQuarterDb
            : snrQuarterDb;

    snrSign =
        snrQuarterDb < 0
            ? "-"
            : "";

    snrWhole =
        absoluteSnr / 4;

    snrFraction =
        (absoluteSnr % 4) * 25;

    PRINTF(
        "\r\n"
        "[LORA] Packet received"
        " | length=%u"
        " | RSSI=%d dBm"
        " | SNR=%s%d.%02d dB\r\n",
        (unsigned int)length,
        (int)rssi,
        snrSign,
        (int)snrWhole,
        (int)snrFraction);

    PRINTF(
        "[LORA] HEX   : ");

    for (index = 0U;
         index < length;
         index++)
    {

        PRINTF(
            "%02X ",
            (unsigned int)buffer[index]);
    }

    PRINTF("\r\n");

    PRINTF(
        "[LORA] ASCII : ");

    for (index = 0U;
         index < length;
         index++)
    {

        uint8_t value;
        char visibleCharacter;

        value =
            buffer[index];

        visibleCharacter =
            value >= 32U && value <= 126U
                ? (char)value
                : '.';

        PRINTF(
            "%c",
            (int)visibleCharacter);
    }

    PRINTF("\r\n");

    /*
     * Payload sensor versi lama teman mempunyai
     * ukuran 21 byte.
     *
     * Untuk saat ini hanya ditandai dan belum
     * langsung diparsing.
     */
    if (length == 22U)
    {
        PRINTF(
            "[LORA] Sensor payload recognized: 22 bytes.\r\n");

        LORA_PrintSensorPayload(
            buffer,
            length);
    }
}

static void LORA_PrintSensorPayload(
    const uint8_t *buffer,
    uint32_t length)
{
    uint8_t nodeId;
    uint8_t nodeTarget;

    int16_t soilMoisture;
    int16_t soilTemperature;
    int16_t conductivity;
    int16_t soilPh;
    int16_t nitrogen;
    int16_t phosphorus;
    int16_t potassium;
    int16_t airTemperature;
    int16_t airHumidity;
    int16_t lightIntensity;

    if (buffer == NULL || length != 22U)
    {
        return;
    }

    nodeId = buffer[0];
    nodeTarget = buffer[1];

    soilMoisture =
        LORA_ReadInt16LE(buffer, 2U);

    soilTemperature =
        LORA_ReadInt16LE(buffer, 4U);

    conductivity =
        LORA_ReadInt16LE(buffer, 6U);

    soilPh =
        LORA_ReadInt16LE(buffer, 8U);

    nitrogen =
        LORA_ReadInt16LE(buffer, 10U);

    phosphorus =
        LORA_ReadInt16LE(buffer, 12U);

    potassium =
        LORA_ReadInt16LE(buffer, 14U);

    airTemperature =
        LORA_ReadInt16LE(buffer, 16U);

    airHumidity =
        LORA_ReadInt16LE(buffer, 18U);

    lightIntensity =
        LORA_ReadInt16LE(buffer, 20U);

    PRINTF(
        "[LORA] Node ID          : %u\r\n",
        (unsigned int)nodeId);

    PRINTF(
        "[LORA] Node Target      : %u\r\n",
        (unsigned int)nodeTarget);

    PRINTF(
        "[LORA] Soil Moisture    : %d.%d\r\n",
        (int)(soilMoisture / 10),
        (int)(soilMoisture >= 0
                  ? soilMoisture % 10
                  : -(soilMoisture % 10)));

    PRINTF(
        "[LORA] Soil Temperature : %d.%d C\r\n",
        (int)(soilTemperature / 10),
        (int)(soilTemperature >= 0
                  ? soilTemperature % 10
                  : -(soilTemperature % 10)));

    PRINTF(
        "[LORA] Conductivity     : %d\r\n",
        (int)conductivity);

    PRINTF(
        "[LORA] Soil pH          : %d.%d\r\n",
        (int)(soilPh / 10),
        (int)(soilPh >= 0
                  ? soilPh % 10
                  : -(soilPh % 10)));

    PRINTF(
        "[LORA] Nitrogen         : %d\r\n",
        (int)nitrogen);

    PRINTF(
        "[LORA] Phosphorus       : %d\r\n",
        (int)phosphorus);

    PRINTF(
        "[LORA] Potassium        : %d\r\n",
        (int)potassium);

    PRINTF(
        "[LORA] Air Temperature  : %d.%d C\r\n",
        (int)(airTemperature / 10),
        (int)(airTemperature >= 0
                  ? airTemperature % 10
                  : -(airTemperature % 10)));

    PRINTF(
        "[LORA] Air Humidity     : %d.%d\r\n",
        (int)(airHumidity / 10),
        (int)(airHumidity >= 0
                  ? airHumidity % 10
                  : -(airHumidity % 10)));

    PRINTF(
        "[LORA] Light Intensity  : %d\r\n",
        (int)lightIntensity);
}

/**
 * FreeRTOS task untuk mendeteksi RFM95W.
 *
 * Task hanya dijalankan sekali, kemudian dihentikan.
 */

/**
 * FreeRTOS task untuk menerima paket LoRa.
 *
 * Receiver menggunakan polling terhadap IRQ RxDone,
 * sehingga pin DIO0 belum diperlukan.
 */
static void lora_test_task(
    void *pvParameters)
{
    uint8_t version;
    uint8_t packetBuffer[RFM95_MAX_PACKET_LENGTH];

    bool registerReadSuccessful;
    bool receiverConfigured;

    int32_t packetLength;
    int32_t packetRssi;
    int32_t packetSnrQuarterDb;

    (void)pvParameters;

    version = 0x00U;

    /*
     * Tunggu startup awal SmartGreenhouse.
     */
    vTaskDelay(
        pdMS_TO_TICKS(5000U));

    PRINTF(
        "\r\n"
        "[LORA] Starting RFM95W receiver...\r\n");

    PRINTF(
        "[LORA] Initializing FLEXCOMM1 / SPI1...\r\n");

    LORA_InitSpi();

    PRINTF(
        "[LORA] Resetting RFM95W...\r\n");

    LORA_Reset();

    PRINTF(
        "[LORA] Reading RegVersion 0x42...\r\n");

    registerReadSuccessful =
        LORA_ReadRegister(
            RFM95_REG_VERSION,
            &version);

    if (!registerReadSuccessful)
    {
        PRINTF(
            "[LORA] ERROR: SPI transfer failed.\r\n");

        vTaskSuspend(NULL);
        return;
    }

    PRINTF(
        "[LORA] RegVersion = 0x%02X\r\n",
        (unsigned int)version);

    if (version != RFM95_EXPECTED_VERSION)
    {
        PRINTF(
            "[LORA] ERROR: RFM95W was not detected.\r\n");

        PRINTF(
            "[LORA] Expected 0x%02X"
            " but received 0x%02X.\r\n",
            (unsigned int)RFM95_EXPECTED_VERSION,
            (unsigned int)version);

        vTaskSuspend(NULL);
        return;
    }

    PRINTF(
        "[LORA] RFM95W detected successfully.\r\n");

    PRINTF(
        "[LORA] Configuring receiver...\r\n");

    receiverConfigured =
        LORA_ConfigureReceiver();

    if (!receiverConfigured)
    {
        PRINTF(
            "[LORA] ERROR:"
            " receiver configuration failed.\r\n");

        vTaskSuspend(NULL);
        return;
    }

    PRINTF(
        "[LORA] RX continuous ready"
        " | frequency=923 MHz"
        " | SF=7"
        " | BW=125 kHz"
        " | CR=4/5"
        " | preamble=8"
        " | sync=0x12"
        " | CRC=off\r\n");

    PRINTF(
        "[LORA] Waiting for packets...\r\n");

    for (;;)
    {
        packetLength =
            LORA_ReceivePacket(
                packetBuffer,
                sizeof(packetBuffer),
                &packetRssi,
                &packetSnrQuarterDb);

        if (packetLength > 0)
        {
            LORA_PrintPacket(
                packetBuffer,
                (uint32_t)packetLength,
                packetRssi,
                packetSnrQuarterDb);

            if (LORA_SaveLatestPayload(
                    packetBuffer,
                    (uint32_t)packetLength,
                    packetRssi,
                    packetSnrQuarterDb))
            {
                PRINTF(
                    "[LORA] Latest sensor payload saved in native memory.\r\n");
            }
        }
        else if (packetLength == -1)
        {
            PRINTF(
                "[LORA] Packet discarded"
                " because CRC error was detected.\r\n");
        }
        else if (packetLength == -2)
        {
            PRINTF(
                "[LORA] ERROR:"
                " SPI/register access failed.\r\n");

            vTaskDelay(
                pdMS_TO_TICKS(500U));
        }

        /*
         * Polling setiap 20 ms.
         * Tidak memerlukan DIO0.
         */
        vTaskDelay(
            pdMS_TO_TICKS(20U));
    }
}

/*!
 * @brief Application entry point.
 */
int main(void)
{
    BOARD_InitBootPins();
    BOARD_InitBootClocks();
#ifdef ENABLE_ETHERNET
    CLOCK_EnableClock(kCLOCK_TddrMciEnetClk);
#endif
    BOARD_InitDebugConsole();

#ifdef ENABLE_WIFI
    /* Reset GMDA */
    RESET_PeripheralReset(kGDMA_RST_SHIFT_RSTn);
    /* Keep CAU sleep clock here. */
    /* CPU1 uses Internal clock when in low power mode. */
    POWER_ConfigCauInSleep(false);
    BOARD_InitSleepPinConfig();
#ifdef RW610
    POWER_PowerOffBle();
#endif
#endif
    BOARD_InitLcdicClock();

    CLOCK_AttachClk(kSFRO_to_FLEXCOMM2);
    /* GPIO. */
    GPIO_PortInit(GPIO, 0);
    GPIO_PortInit(GPIO, 1);
    INPUTMUX_Init(INPUTMUX);
    RESET_PeripheralReset(kINPUTMUX_RST_SHIFT_RSTn);

    INPUTMUX_AttachSignal(INPUTMUX, APP_LCD_TX_DMA_CH, kINPUTMUX_LcdTxRegToDmaSingleToDma0);

    INPUTMUX_EnableSignal(INPUTMUX, kINPUTMUX_Dmac0InputTriggerLcdTxRegToDmaSingleEna, true);

#if ENABLE_SYSTEM_VIEW == 1
    /* start_sysview_logging */
    SEGGER_SYSVIEW_Conf();
    PRINTF("SEGGER_RTT block address: %p\n", &(_SEGGER_RTT));
    SEGGER_SYSVIEW_setMicroJVMTask((U32)pvMicrojvmCreatedTask);
#endif

    /* Start NXP Platform Accelerator task. */
    if (xTaskCreate(nxp_pa_task, "NXP_PA_task", 4 * 1024, NULL, nxp_pa_task_PRIORITY, &pvMicrojvmCreatedTask) !=
        pdPASS)
    {
        PRINTF("Task creation failed!.\r\n");
        while (1)
            ;
    }

    /*
     * Task deteksi RFM95W.
     *
     * Task ini tidak berinteraksi dengan Java,
     * Wi-Fi, GUI, maupun MQTT.
     */
     PRINTF(
             "[MEM] Before LoRa task"
             " | free=%u"
             " | minimum=%u\r\n",
             (unsigned int)xPortGetFreeHeapSize(),
             (unsigned int)xPortGetMinimumEverFreeHeapSize()
     );

    if (xTaskCreate(
            lora_test_task,
            "LoRa_Test",
            LORA_TEST_TASK_STACK_SIZE,
            NULL,
            lora_test_task_PRIORITY,
            NULL) != pdPASS)
    {

        PRINTF(
            "[LORA] ERROR: task creation failed.\r\n");
    }

    PRINTF(
            "[MEM] After LoRa task"
            " | free=%u"
            " | minimum=%u\r\n",
            (unsigned int)xPortGetFreeHeapSize(),
            (unsigned int)xPortGetMinimumEverFreeHeapSize()
    );

    /* Enable crypto accelerator */
    status_t status = CRYPTO_InitHardware();
    // assert(status == kStatus_Success);
    if (status != kStatus_Success)
    {
        PRINTF(
            "CRYPTO hardware initialization failed.\r\n");

        while (1)
        {
        }
    }

    vTaskStartScheduler();
    for (;;)
        ;
}

static void BOARD_InitLcdicClock()
{
    /* LCDIC clock.
     * SPI baud rate is the same with LCDIC functional clock.
     */
    CLOCK_EnableClock(kCLOCK_T3PllMciFlexspiClk);
    PRINTF("kMAIN_CLK_to_LCD_CLK %d\n", kMAIN_CLK_to_LCD_CLK);
    CLOCK_AttachClk(kT3PLL_MCI_FLEXSPI_to_LCD_CLK);
    CLOCK_SetClkDiv(kCLOCK_DivLcdClk, 12);
    RESET_PeripheralReset(kLCDIC_RST_SHIFT_RSTn);
}

void microjvm_main(void)
{
    void *vm;
    int32_t err;
    int32_t exitcode;

    // create VM
    vm = SNI_createVM();

    if (vm == NULL)
    {
        PRINTF("VM initialization error.\n");
    }
    else
    {
        PRINTF("VM START\n");
        err = SNI_startVM(vm, 0, NULL);

        if (err < 0)
        {
            // Error occurred
            if (err == LLMJVM_E_EVAL_LIMIT)
            {
                PRINTF("Evaluation limits reached.\n");
            }
            else
            {
                PRINTF("VM execution error (err = %d).\n", -err);
            }
        }
        else
        {
            // VM execution ends normally
            exitcode = SNI_getExitCode(vm);
            PRINTF("VM END (exit code = %d)\n", exitcode);
        }

        // delete VM
        SNI_destroyVM(vm);
    }
}

/*!
 * @brief Task responsible for printing of "Hello world." message.
 */
extern char _HeapAsFreeRAMSize __asm("_HeapAsFreeRAMSize");
static void nxp_pa_task(void *pvParameters)
{
#ifdef CPULOAD_ENABLED
    /* Start the CPU Load task */
    cpuload_init();
#endif

#ifdef SD_ENABLED
    /* Start SD Card task. */
    START_SDCARD_Task(NULL);
#endif

    for (;;)
    {
        PRINTF("\r\nNXP PLATFORM ACCELERATOR\r\n");
        PRINTF("NXP VEE Port '%s' '%s'\r\n", VEE_VERSION, GIT_SHA_1);
        PRINTF("NXP VEE Heap size: %u Bytes\r\n", (unsigned)&_HeapAsFreeRAMSize);

#if (ENABLE_SHELL == 1)
        if (shell_init() != 0)
            PRINTF("Could not run shell...\r\n");
#endif

        microjvm_main();
        vTaskSuspend(NULL);
    }
}

void vApplicationMallocFailedHook()
{
    PRINTF(("\r\nERROR: Malloc failed to allocate memory\r\n"));

    /* Loop forever */
    for (;;)
        ;
}

/*!
 * @brief Call the cpuload_idle function in FreeRTOS ilde hook function
 */

void vApplicationIdleHook(void)
{
#ifdef CPULOAD_ENABLED
    cpuload_idle();
#endif
}