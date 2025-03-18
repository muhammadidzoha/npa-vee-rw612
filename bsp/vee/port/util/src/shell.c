/*
 * Copyright 2024 NXP
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>

#include "FreeRTOS.h"
#include "task.h"

#include "fsl_debug_console.h"
#include "fsl_component_serial_manager.h"
#include "fsl_shell.h"

#include "shell.h"

/*******************************************************************************
 * Definitions
 ******************************************************************************/

/*******************************************************************************
 * Prototypes
 ******************************************************************************/

/*******************************************************************************
 * Variables
 ******************************************************************************/
SDK_ALIGN(static uint8_t s_shellHandleBuffer[SHELL_HANDLE_SIZE], 4);
static shell_handle_t s_shellHandle;

extern serial_handle_t g_serialHandle;

/*******************************************************************************
 * Code
 ******************************************************************************/
int shell_init(void)
{
    int ret = 0;

    /* Init SHELL */
    s_shellHandle = &s_shellHandleBuffer[0];

    SHELL_Init(s_shellHandle, g_serialHandle, "NXP PA > ");

    /* Add new command to commands list */

    return ret;
}
