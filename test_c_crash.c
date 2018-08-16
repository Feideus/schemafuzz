#include<stdio.h>
#include<stdlib.h>
#include<time.h>

void tmpfun();
void tmpfun2();

int main(int argc, char** argv)
{
	srand(time(NULL));
	int random = rand()%2;
	printf("crash test");
	if(random == 1)
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
