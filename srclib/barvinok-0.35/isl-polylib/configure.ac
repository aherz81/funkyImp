AC_INIT([isl-polylib], [0.01], [isl-development@googlegroups.com])
AC_CONFIG_AUX_DIR([.])
AC_CONFIG_MACRO_DIR([m4])
AM_INIT_AUTOMAKE([foreign])
m4_ifdef([AM_SILENT_RULES],[AM_SILENT_RULES([yes])])
AC_SUBST(versioninfo)
versioninfo=1:0:0

AC_PROG_CC
AC_PROG_LIBTOOL

AX_SUBMODULE(gmp,system,system)

AC_SUBST(GMP_CPPFLAGS)
AC_SUBST(GMP_LDFLAGS)
if test "x$with_gmp_prefix" != "x"; then
	GMP_CPPFLAGS="-I$with_gmp_prefix/include"
fi
if test "x$with_gmp_exec_prefix" != "x"; then
	GMP_LDFLAGS="-L$with_gmp_exec_prefix/lib"
fi

AX_SUBMODULE(isl,system|build|bundled,bundled)

AX_SUBMODULE(polylib,system|build|bundled,bundled)
AC_SUBST(POLYLIB_CPPFLAGS)
AC_SUBST(POLYLIB_LDFLAGS)
AC_SUBST(POLYLIB_LIBS)
case "$with_polylib" in
	bundled)
		POLYLIB_CPPFLAGS="-I$srcdir/polylib/include"
		if test "x$with_gmp_prefix" != "x"; then
			isl_polylib_configure_args="$isl_polylib_configure_args --with-libgmp=$with_gmp_prefix"
		else
			isl_polylib_configure_args="$isl_polylib_configure_args --with-libgmp"
		fi
	;;
	build)
		polylibs=`echo @polylibs@ | $with_polylib_builddir/config.status --file=-`
		AC_MSG_NOTICE(Configured polylibs: $polylibs)
		isl_cv_polylib=missing
		for bits in $polylibs; do
			if test "$bits" = "libpolylibgmp.la"; then
				isl_cv_polylib=ok
			fi
		done
		if test "$isl_cv_polylib" = "missing"; then
			AC_MSG_ERROR(no gmp polylib configured)
		fi
		POLYLIB_CPPFLAGS="-I$with_polylib_builddir/include -I$polylib_srcdir/include"
		POLYLIB_LIBS="$with_polylib_builddir/libpolylibgmp.la"
	;;
	system)
		POLYLIB_LIBS="-lpolylibgmp"
		if test "x$with_polylib_prefix" != "x"; then
			POLYLIB_CPPFLAGS="-I$with_polylib_prefix/include"
			POLYLIB_LDFLAGS="-L$with_polylib_prefix/lib"
		fi
		SAVE_CPPFLAGS="$CPPFLAGS"
		SAVE_LDFLAGS="$LDFLAGS"
		CPPFLAGS="$POLYLIB_CPPFLAGS $CPPFLAGS"
		LDFLAGS="$POLYLIB_LDFLAGS $LDFLAGS"
		AC_CHECK_LIB(polylibgmp, PolyhedronTSort,[ true ],[
			AC_MSG_ERROR(Need polylib)
		])
		CPPFLAGS="$SAVE_CPPFLAGS"
		LDFLAGS="$SAVE_LDFLAGS"
	;;
	*)
		AC_MSG_ERROR(unsupported)
	;;
esac
AM_CONDITIONAL(BUNDLED_POLYLIB, test $with_polylib = bundled)

AC_SUBST(ISL_CFLAGS)
AC_SUBST(ISL_LIBS)
case "$with_isl" in
bundled)
	ISL_CFLAGS="-I$srcdir/isl/include -Iisl/include"
	;;
build)
	ISL_CFLAGS="-I$isl_srcdir/include -I$with_isl_builddir/include"
	ISL_LIBS="$with_isl_builddir/libisl.la"
	;;
system)
	if test "x$with_isl_exec_prefix" != "x"; then
		export PKG_CONFIG_PATH=$with_isl_exec_prefix/lib/pkgconfig:$PKG_CONFIG_PATH
	fi
	PKG_CHECK_MODULES([ISL], [isl])
esac
AM_CONDITIONAL(BUNDLED_ISL, test $with_isl = bundled)

PACKAGE_CFLAGS="-I\${prefix}/include/isl-polylib -I\${prefix}/include"
AX_CREATE_PKGCONFIG_INFO

AC_CONFIG_FILES(Makefile)
if test $with_isl = bundled; then
    AC_CONFIG_SUBDIRS(isl)
fi
if test $with_polylib = bundled; then
    AC_CONFIG_SUBDIRS(polylib)
fi
AC_CONFIG_COMMANDS_POST([
	dnl pass on arguments to subdir configures, but don't
	dnl add them to config.status
	ac_configure_args="$ac_configure_args $isl_polylib_configure_args --with-bits=gmp"
])
AC_OUTPUT
