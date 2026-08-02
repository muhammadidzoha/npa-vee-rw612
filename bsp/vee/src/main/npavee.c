#include "FreeRTOS.h"
#include "task.h"
#include "queue.h"
#include "timers.h"

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

#include "fsl_lcdic.h"
#include "fsl_lcdic_dma.h"
#include "panel_func.h"

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
#include "lora.h"
#include "ds3231.h"

#define nxp_pa_task_PRIORITY (configMAX_PRIORITIES - 6)
#define rtc_test_task_PRIORITY (tskIDLE_PRIORITY + 1)

#define RTC_FORCE_TEST_SET 1

static void nxp_pa_task(void *pvParameters);
static void rtc_test_task(void *pvParameters);

static void BOARD_InitLcdicClock();

TaskHandle_t pvMicrojvmCreatedTask = NULL;

int main(void)
{
    BOARD_InitBootPins();
    BOARD_InitBootClocks();

#ifdef ENABLE_ETHERNET
    CLOCK_EnableClock(kCLOCK_TddrMciEnetClk);
#endif

    BOARD_InitDebugConsole();

#ifdef ENABLE_WIFI

    RESET_PeripheralReset(kGDMA_RST_SHIFT_RSTn);

    POWER_ConfigCauInSleep(false);
    BOARD_InitSleepPinConfig();

#ifdef RW610
    POWER_PowerOffBle();
#endif

#endif

    BOARD_InitLcdicClock();

    CLOCK_AttachClk(kSFRO_to_FLEXCOMM2);

    GPIO_PortInit(GPIO, 0);
    GPIO_PortInit(GPIO, 1);

    INPUTMUX_Init(INPUTMUX);

    RESET_PeripheralReset(
        kINPUTMUX_RST_SHIFT_RSTn
    );

    INPUTMUX_AttachSignal(
        INPUTMUX,
        APP_LCD_TX_DMA_CH,
        kINPUTMUX_LcdTxRegToDmaSingleToDma0
    );

    INPUTMUX_EnableSignal(
        INPUTMUX,
        kINPUTMUX_Dmac0InputTriggerLcdTxRegToDmaSingleEna,
        true
    );

#if ENABLE_SYSTEM_VIEW == 1

    SEGGER_SYSVIEW_Conf();

    PRINTF(
        "SEGGER_RTT block address: %p\n",
        &(_SEGGER_RTT)
    );

    SEGGER_SYSVIEW_setMicroJVMTask(
        (U32)pvMicrojvmCreatedTask
    );

#endif

    if (xTaskCreate(
            nxp_pa_task,
            "NXP_PA_task",
            4 * 1024,
            NULL,
            nxp_pa_task_PRIORITY,
            &pvMicrojvmCreatedTask) != pdPASS)
    {
        PRINTF(
            "Task creation failed!.\r\n"
        );

        while (1)
            ;
    }

    /*
     * RTC tidak dites langsung di main().
     *
     * RTC dijalankan pada task terpisah dengan priority rendah.
     * Task akan menunggu 5 detik supaya MicroJVM dan UI dapat
     * berjalan terlebih dahulu.
     */
    if (xTaskCreate(
            rtc_test_task,
            "RTC_test",
            2 * 1024,
            NULL,
            rtc_test_task_PRIORITY,
            NULL) != pdPASS)
    {
        PRINTF(
            "[RTC] ERROR: RTC test task creation failed.\r\n"
        );
    }

    LORA_Start();

    status_t status =
        CRYPTO_InitHardware();

    if (status != kStatus_Success)
    {
        PRINTF(
            "CRYPTO hardware initialization failed.\r\n"
        );

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
    CLOCK_EnableClock(
        kCLOCK_T3PllMciFlexspiClk
    );

    PRINTF(
        "kMAIN_CLK_to_LCD_CLK %d\n",
        kMAIN_CLK_to_LCD_CLK
    );

    CLOCK_AttachClk(
        kT3PLL_MCI_FLEXSPI_to_LCD_CLK
    );

    CLOCK_SetClkDiv(
        kCLOCK_DivLcdClk,
        12
    );

    RESET_PeripheralReset(
        kLCDIC_RST_SHIFT_RSTn
    );
}

void microjvm_main(void)
{
    void *vm;
    int32_t err;
    int32_t exitcode;

    vm =
        SNI_createVM();

    if (vm == NULL)
    {
        PRINTF(
            "VM initialization error.\n"
        );
    }
    else
    {
        PRINTF(
            "VM START\n"
        );

        err =
            SNI_startVM(
                vm,
                0,
                NULL
            );

        if (err < 0)
        {
            if (err == LLMJVM_E_EVAL_LIMIT)
            {
                PRINTF(
                    "Evaluation limits reached.\n"
                );
            }
            else
            {
                PRINTF(
                    "VM execution error (err = %d).\n",
                    -err
                );
            }
        }
        else
        {
            exitcode =
                SNI_getExitCode(
                    vm
                );

            PRINTF(
                "VM END (exit code = %d)\n",
                exitcode
            );
        }

        SNI_destroyVM(
            vm
        );
    }
}

extern char _HeapAsFreeRAMSize
    __asm("_HeapAsFreeRAMSize");

static void nxp_pa_task(
    void *pvParameters)
{
    (void)pvParameters;

#ifdef CPULOAD_ENABLED

    cpuload_init();

#endif

#ifdef SD_ENABLED

    START_SDCARD_Task(
        NULL
    );

#endif

    for (;;)
    {
        PRINTF(
            "\r\nNXP PLATFORM ACCELERATOR\r\n"
        );

        PRINTF(
            "NXP VEE Port '%s' '%s'\r\n",
            VEE_VERSION,
            GIT_SHA_1
        );

        PRINTF(
            "NXP VEE Heap size: %u Bytes\r\n",
            (unsigned)&_HeapAsFreeRAMSize
        );

#if (ENABLE_SHELL == 1)

        if (shell_init() != 0)
        {
            PRINTF(
                "Could not run shell...\r\n"
            );
        }

#endif

        microjvm_main();

        vTaskSuspend(
            NULL
        );
    }
}

/*
 * RTC TEST TASK
 *
 * Untuk tahap sekarang kita hanya melakukan:
 *
 * 1. Tunggu 5 detik.
 * 2. Initialize I2C2.
 * 3. Read register seconds sekali.
 * 4. Tunggu 1 detik.
 * 5. Read register seconds sekali lagi.
 *
 * Belum membaca menit/jam/tanggal.
 * Belum menulis RTC.
 * Belum integrasi NTP.
 */
static void rtc_test_task(
    void *pvParameters)
{
    ds3231_datetime_t rtcDateTime;

    (void)pvParameters;

    /*
     * Tunggu MicroJVM / UI berjalan dahulu.
     */
    vTaskDelay(
        pdMS_TO_TICKS(5000)
    );

    PRINTF(
        "\r\n[RTC] =====================================\r\n"
    );

    PRINTF(
        "[RTC] Starting DS3231 write/persistence test\r\n"
    );

    PRINTF(
        "[RTC] =====================================\r\n"
    );

    /*
     * Initialize I2C2.
     */
    if (!DS3231_Init())
    {
        PRINTF(
            "[RTC] ERROR: DS3231 initialization failed.\r\n"
        );

        vTaskDelete(NULL);
        return;
    }

    /*
     * Pastikan device masih tersedia.
     */
    PRINTF(
        "\r\n[RTC] TEST 1: Communication\r\n"
    );

    if (!DS3231_TestCommunication())
    {
        PRINTF(
            "[RTC] ERROR: Communication test failed.\r\n"
        );

        vTaskDelete(NULL);
        return;
    }

    PRINTF(
        "[RTC] TEST 1 PASSED\r\n"
    );

    /*
     * Cek kondisi OSF sebelum write.
     */
    PRINTF(
        "\r\n[RTC] TEST 2: RTC validity before write\r\n"
    );

    bool validBeforeWrite =
        DS3231_IsTimeValid();

    PRINTF(
        "[RTC] RTC valid before write = %d\r\n",
        validBeforeWrite ? 1 : 0
    );

#if RTC_FORCE_TEST_SET == 1

    /*
     * ==========================================================
     * TEST SET
     *
     * Ini hanya digunakan untuk tahap pengujian.
     *
     * 1 = tulis waktu contoh ke DS3231.
     * 0 = JANGAN menulis RTC pada boot.
     *
     * Setelah test write berhasil, ubah
     * RTC_FORCE_TEST_SET menjadi 0.
     * ==========================================================
     */

    PRINTF(
        "\r\n[RTC] TEST 3: Writing test date/time\r\n"
    );

    rtcDateTime.year = 2026U;
    rtcDateTime.month = 8U;
    rtcDateTime.date = 2U;

    /*
     * Kita gunakan:
     * 1 = Sunday
     * 2 = Monday
     * ...
     * 7 = Saturday
     *
     * 2 Agustus 2026 = Sunday.
     */
    rtcDateTime.day = 1U;

    rtcDateTime.hour = 12U;
    rtcDateTime.minute = 0U;
    rtcDateTime.second = 0U;

    PRINTF(
        "[RTC] Test value to write:\r\n"
    );

    DS3231_PrintDateTime(
        &rtcDateTime
    );

    if (!DS3231_SetDateTime(
            &rtcDateTime))
    {
        PRINTF(
            "[RTC] ERROR: Date/time write failed.\r\n"
        );

        vTaskDelete(NULL);
        return;
    }

    PRINTF(
        "[RTC] TEST 3 PASSED\r\n"
    );

    /*
     * Tunggu 3 detik.
     * Setelah itu RTC seharusnya sudah sekitar 12:00:03.
     */
    PRINTF(
        "\r\n[RTC] Waiting 3 seconds...\r\n"
    );

    vTaskDelay(
        pdMS_TO_TICKS(3000)
    );

#else

    PRINTF(
        "\r\n[RTC] RTC_FORCE_TEST_SET = 0\r\n"
    );

    PRINTF(
        "[RTC] RTC will NOT be overwritten on this boot.\r\n"
    );

#endif

    /*
     * Baca RTC.
     */
    PRINTF(
        "\r\n[RTC] TEST 4: Reading date/time\r\n"
    );

    if (!DS3231_ReadDateTime(
            &rtcDateTime))
    {
        PRINTF(
            "[RTC] ERROR: Unable to read date/time.\r\n"
        );

        vTaskDelete(NULL);
        return;
    }

    DS3231_PrintDateTime(
        &rtcDateTime
    );

    PRINTF(
        "[RTC] TEST 4 PASSED\r\n"
    );

    /*
     * Cek OSF setelah write.
     */
    PRINTF(
        "\r\n[RTC] TEST 5: RTC validity after write\r\n"
    );

    if (DS3231_IsTimeValid())
    {
        PRINTF(
            "[RTC] TEST 5 PASSED | RTC is VALID\r\n"
        );
    }
    else
    {
        PRINTF(
            "[RTC] TEST 5 FAILED | RTC is INVALID\r\n"
        );
    }

    PRINTF(
        "\r\n[RTC] =====================================\r\n"
    );

    PRINTF(
        "[RTC] RTC test completed\r\n"
    );

#if RTC_FORCE_TEST_SET == 1

    PRINTF(
        "[RTC] IMPORTANT: Change RTC_FORCE_TEST_SET to 0\r\n"
    );

    PRINTF(
        "[RTC] before performing the power-off persistence test.\r\n"
    );

#endif

    PRINTF(
        "[RTC] =====================================\r\n"
    );

    vTaskDelete(
        NULL
    );
}

void vApplicationMallocFailedHook()
{
    PRINTF(
        (
            "\r\nERROR: Malloc failed to allocate memory\r\n"
        )
    );

    for (;;)
        ;
}

void vApplicationIdleHook(void)
{
#ifdef CPULOAD_ENABLED

    cpuload_idle();

#endif
}