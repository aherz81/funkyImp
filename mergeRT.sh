rm $1/funkyimp/lib/libFunkyCompleteRunTime.a
find $1/funkyimp/lib -name lib*.a | xargs -I{} -t ar -x {}
ar -r $1/funkyimp/lib/libFunkyCompleteRunTime.a *.o
rm *.o
ranlib $1/funkyimp/lib/libFunkyCompleteRunTime.a
