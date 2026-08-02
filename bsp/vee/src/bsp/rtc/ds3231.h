#ifndef DS3231_H
#define DS3231_H

#include <stdbool.h>

bool DS3231_Init(void);
bool DS3231_TestCommunication(void);

#endif