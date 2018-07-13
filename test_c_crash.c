#include<stdio.h>
#include<stdlib.h>

void tmpfun();
void tmpfun2();

int main(int argc, char** argv)
{
	printf("crash test");
	tmpfun();
	return EXIT_SUCCESS;
}

void tmpfun()
{
    tmpfun2();
}

void tmpfun2()
{
    *(char *)0 = 0;
}
fileNames:
test_c_crash.c,
fileNames:
test_c_crash.c,
fileNames:
test_c_crash.c,
fileNames:
test_c_crash.c,
fileNames:
test_c_crash.c,
fileNames:
test_c_crash.c,
:
test_c_crash.c,
:
test_c_crash.c,
:
test_c_crash.c,
fileNames:
test_c_crash.c,
fileNames:
errorReports/parsedStackTrace_30,
