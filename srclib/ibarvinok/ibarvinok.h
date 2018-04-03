/*
 * File:   ibarvinok.h
 * Author: aherz
 *
 * Created on 23. Oktober 2012, 12:53
 */

#ifndef IBARVINOK_H
#define	IBARVINOK_H

#ifdef	__cplusplus
extern "C"
{
#endif

extern void init(int dump);
extern char* process_line(char* input);
extern void error_dump(int doit);
extern void shutdown();
extern void freeres(char* string);

#ifdef	__cplusplus
}
#endif

#endif	/* IBARVINOK_H */

