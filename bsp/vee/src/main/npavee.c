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
     * Tunggu MicroJVM / UI start dahulu.
     */
    vTaskDelay(
        pdMS_TO_TICKS(5000)
    );

    PRINTF(
        "\r\n[RTC] =============================\r\n"
    );

    PRINTF(
        "[RTC] Starting DS3231 date/time test\r\n"
    );

    PRINTF(
        "[RTC] =============================\r\n"
    );

    /*
     * Initialize I2C2.
     */
    if (!DS3231_Init())
    {
        PRINTF(
            "[RTC] ERROR: DS3231 initialization failed.\r\n"
        );

        vTaskDelete(
            NULL
        );

        return;
    }

    /*
     * Pertama pastikan device masih terdeteksi.
     */
    PRINTF(
        "\r\n[RTC] TEST 1: Communication\r\n"
    );

    if (!DS3231_TestCommunication())
    {
        PRINTF(
            "[RTC] ERROR: Communication test failed.\r\n"
        );

        vTaskDelete(
            NULL
        );

        return;
    }

    PRINTF(
        "[RTC] TEST 1 PASSED\r\n"
    );

    /*
     * Beri jeda sebelum melakukan rangkaian
     * transaksi berikutnya.
     */
    vTaskDelay(
        pdMS_TO_TICKS(1000)
    );

    /*
     * Full date/time read.
     */
    PRINTF(
        "\r\n[RTC] TEST 2: Full date/time read\r\n"
    );

    if (!DS3231_ReadDateTime(&rtcDateTime))
    {
        PRINTF(
            "[RTC] ERROR: Full date/time read failed.\r\n"
        );

        vTaskDelete(
            NULL
        );

        return;
    }

    PRINTF(
        "[RTC] TEST 2 register reads PASSED\r\n"
    );

    /*
     * Print hasil akhirnya.
     */
    DS3231_PrintDateTime(
        &rtcDateTime
    );

    PRINTF(
        "[RTC] Full date/time test PASSED.\r\n"
    );

    PRINTF(
        "[RTC] RTC task finished.\r\n"
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