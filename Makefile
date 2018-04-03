default:
	 $(MAKE) -C ./backend/RunTime
	 $(MAKE) -C ./backend/boehm/bdwgc/
	 $(MAKE) -C ./backend/tbb4/
	 $(MAKE) -C ./srclib/barvinok-0.35/
	 find ./srclib/barvinok-0.35 -name *.dylib* | xargs -I{} rm {}
	 find ./srclib/barvinok-0.35 -name *.so* | xargs -I{} rm {}
	 $(MAKE) -C ./srclib/ibarvinok
	 $(MAKE) -C ./javac/langtools/make/

clean:
	 $(MAKE) clean -C ./backend/RunTime
	 $(MAKE) clean -C ./backend/boehm/bdwgc/
	 $(MAKE) clean -C ./backend/tbb4/
	 $(MAKE) clean -C ./srclib/barvinok-0.35
	 $(MAKE) clean -C ./srclib/ibarvinok
	 $(MAKE) clean -C ./javac/langtools/make/

install:
	 $(MAKE) install -C ./backend/RunTime
	 $(MAKE) install -C ./backend/boehm/bdwgc/
	 $(MAKE) install -C ./backend/tbb4/
	 $(MAKE) install -C ./srclib/barvinok-0.35
	 $(MAKE) install -C ./srclib/ibarvinok
	 $(MAKE) -C ./javac/langtools/make/	 
	 echo "shared libs have been installed in '/usr/local/lib'"
	 echo "add /usr/local/lib to LD_LIBRARY_PATH"
	 
uninstall:
	 $(MAKE) uninstall -C ./backend/RunTime
	 $(MAKE) uninstall -C ./backend/boehm/bdwgc/
	 $(MAKE) uninstall -C ./backend/tbb4/
	 $(MAKE) uninstall -C ./srclib/barvinok-0.35
	 $(MAKE) uninstall -C ./srclib/ibarvinok

