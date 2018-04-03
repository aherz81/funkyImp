if BUNDLED_ISL
    MAYBE_ISL = isl
    ISL_LA = $(top_builddir)/isl/libisl.la
endif
if BUNDLED_POLYLIB
    MAYBE_POLYLIB = polylib
    POLYLIB_LA = $(top_builddir)/polylib/libpolylibgmp.la
endif

SUBDIRS = $(MAYBE_ISL) $(MAYBE_POLYLIB) .
DIST_SUBDIRS = $(MAYBE_ISL) $(MAYBE_POLYLIB)

ACLOCAL_AMFLAGS = -I m4

LIB_ISL = $(ISL_LA) @ISL_LIBS@
LIB_POLYLIB = $(POLYLIB_LA) @POLYLIB_LDFLAGS@ @POLYLIB_LIBS@

FORCE:
isl/libisl.la: FORCE
	cd isl; $(MAKE) $(AM_MAKEFLAGS) libisl.la
polylib/libpolylibgmp.la: FORCE
	cd polylib; $(MAKE) $(AM_MAKEFLAGS) libpolylibgmp.la

lib_LTLIBRARIES = libisl-polylib.la

libisl_polylib_la_SOURCES = \
	isl_map_polylib.c
libisl_polylib_la_CPPFLAGS = -I$(srcdir)/include -Iinclude/ \
	$(ISL_CFLAGS) $(POLYLIB_CPPFLAGS) $(GMP_CPPFLAGS)
libisl_polylib_la_LIBADD = $(LTLIBOBJS) $(LIB_ISL) $(LIB_POLYLIB)

pkginclude_HEADERS = \
	include/isl_map_polylib.h \
	include/isl_polylib.h \
	include/isl_set_polylib.h

pkgconfigdir=$(pkgconfig_libdir)
pkgconfig_DATA = $(pkgconfig_libfile)
