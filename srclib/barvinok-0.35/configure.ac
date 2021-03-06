AC_INIT([barvinok], [0.35], [isl-development@googlegroups.com])
AC_CONFIG_MACRO_DIR([m4])
AM_INIT_AUTOMAKE([foreign])
m4_ifdef([AM_SILENT_RULES],[AM_SILENT_RULES([yes])])
AC_PROG_CC

AC_PROG_CXX
AM_PROG_LEX
AC_PROG_YACC
AC_SUBST(versioninfo)
versioninfo=23:0:0

AC_CANONICAL_HOST
AC_LIBTOOL_WIN32_DLL
AC_SUBST(BV_LDFLAGS)
case $host in
	*-*-cygwin* | *-*-mingw* | *-*-pw32* | *-*-os2*)
	if test "$enable_shared" = yes; then
		BV_LDFLAGS="-no-undefined" 
	fi
	;;
esac

AC_PROG_LIBTOOL

PKG_PROG_PKG_CONFIG

AC_CHECK_HEADERS(getopt.h)
AC_CHECK_HEADERS(sys/times.h)
AC_CHECK_FUNCS(sigaction)

AC_MSG_CHECKING(whether to build shared libbarvinok)
AC_ARG_ENABLE(shared_barvinok,
	AS_HELP_STRING([--enable-shared-barvinok],
		       [build shared barvinok library]),
	[bv_cv_shared_barvinok=$enableval], [bv_cv_shared_barvinok="no"])
AC_MSG_RESULT($bv_cv_shared_barvinok)
if test "x$bv_cv_shared_barvinok" != "xyes" ; then
	BV_LDFLAGS="$BV_LDFLAGS -static"
fi

AC_CXX_GNUCXX_HASHMAP

AC_ARG_WITH(default-prefix,
	AS_HELP_STRING([--with-default-prefix=DIR],
	    [Default installation prefix of optional packages]))
if test "${with_default_prefix+set}" = set; then
	if test "${with_libgmp+set}" != set -a \
	   -f "$with_default_prefix/include/gmp.h"; then
		with_libgmp=$with_default_prefix
	fi
	if test "${with_ntl+set}" != set -a \
	   -f "$with_default_prefix/include/NTL/ZZ.h"; then
		with_ntl=$with_default_prefix
	fi
	if test "${with_cddlib+set}" != set -a \
	   -f "$with_default_prefix/include/cddmp.h"; then
		with_cddlib=$with_default_prefix
	fi
	if test "${with_glpk+set}" != set -a \
	   -f "$with_default_prefix/include/glpk.h"; then
		with_glpk=$with_default_prefix
	fi
	if test "${with_topcom+set}" != set -a \
	   -f "$with_default_prefix/bin/points2triangs"; then
		with_topcom=$with_default_prefix
	fi
fi

dnl Check for GMP library
AC_MSG_CHECKING(whether to use GMP)
AC_ARG_WITH(libgmp, 
        [  --with-libgmp           DIR Location of the GMP Distribution], 
	gmp_package=$withval, gmp_package=yes)
if test "x$gmp_package" = "xno"; then
	AC_MSG_RESULT(no)
	AC_MSG_ERROR(Need gmp)
else
	AC_MSG_RESULT(yes)
	if test "x$gmp_package" = "xyes"; then
		bv_configure_args="$bv_configure_args --with-libgmp --with-gmp"
	else
	    bv_configure_args="$bv_configure_args --with-libgmp=$gmp_package"
	    bv_configure_args="$bv_configure_args --with-gmp-prefix=$gmp_package"
	    GMP_DIR=$gmp_package
	    if test ! -d "$GMP_DIR"; then
	    	AC_ERROR(Directory given for GMP Distribution is not a directory)
	    fi
	    CPPFLAGS="-I$GMP_DIR/include $CPPFLAGS"
	    LDFLAGS="-L$GMP_DIR/lib $LDFLAGS"
	fi
	poly_cv_gmpfatal="no"
	AC_CHECK_HEADER(gmp.h,
		[AC_CHECK_LIB(gmp,main,
		    [],
		    [poly_cv_gmpfatal="yes"])],
		[poly_cv_gmpfatal="yes"])
	if test "$poly_cv_gmpfatal" = "yes"; then
		AC_MSG_ERROR([GMP not found])
	fi
fi
AC_CHECK_DECL(mpz_divisible_p,[],[AC_LIBOBJ(mpz_divisible_p)],[#include <gmp.h>])

AX_SUBMODULE(polylib,build|bundled|system,bundled)

AC_SUBST(POLYLIB_CPPFLAGS)
AC_SUBST(POLYLIB_LDFLAGS)
AC_SUBST(POLYLIB_LIBS)
case "$with_polylib" in
bundled)
	bv_configure_args="$bv_configure_args --with-polylib-builddir=../polylib"
	POLYLIB_CPPFLAGS="-I\$(top_builddir)/polylib/include -I\$(top_srcdir)/polylib/include"
	;;
build)
	with_polylib_builddir=`echo @abs_builddir@ | $with_polylib_builddir/config.status --file=-`
	POLYLIB_CPPFLAGS="-I$polylib_srcdir/include -I$with_polylib_builddir/include"
	POLYLIB_LIBS="$with_polylib_builddir/libpolylibgmp.la"
	;;
system)
	POLYLIB_LIBS="-lpolylibgmp"
	if test "x$with_polylib_prefix" != "x"; then
		POLYLIB_CPPFLAGS="-I$with_polylib_prefix/include"
		POLYLIB_LDFLAGS="-L$with_polylib_prefix/lib"
	fi
	SAVE_CPPFLAGS="$CPPFLAGS"
	CPPFLAGS="$POLYLIB_CPPFLAGS $CPPFLAGS"
	AC_CHECK_HEADERS([polylib/polylibgmp.h], [], [
		AC_MSG_ERROR(Need PolyLib)
	])
	CPPFLAGS="$SAVE_CPPFLAGS"
esac

AM_CONDITIONAL(BUNDLED_POLYLIB, test $with_polylib = bundled)

AX_SUBMODULE(isl,build|bundled|system,bundled)

AC_SUBST(ISL_CFLAGS)
AC_SUBST(ISL_LIBS)
AC_SUBST(ISL_BUILDDIR)
case "$with_isl" in
bundled)
	ISL_BUILDDIR=isl
	ISL_CFLAGS="-I\$(top_srcdir)/isl/include -I\$(top_builddir)/isl/include"
	bv_configure_args="$bv_configure_args --with-isl-builddir=../isl"
	PACKAGE_CFLAGS_ISL='-I${prefix}/include'
	;;
build)
	ISL_BUILDDIR=`echo @abs_builddir@ | $with_isl_builddir/config.status --file=-`
	bv_configure_args="$bv_configure_args --with-isl-builddir=$ISL_BUILDDIR"
	ISL_CFLAGS="-I$isl_srcdir/include -I$ISL_BUILDDIR/include"
	PACKAGE_CFLAGS_ISL='-I${prefix}/include'
	;;
system)
	PKG_CHECK_MODULES([ISL], [isl])
	PACKAGE_CFLAGS_ISL="$ISL_CFLAGS"
esac
AM_CONDITIONAL(BUNDLED_ISL, test $with_isl = bundled)
AM_CONDITIONAL(BUILD_ISL, test $with_isl = build)

AX_SUBMODULE(pet,bundled|build|system|no,no)
AC_SUBST(PET_CFLAGS)
AC_SUBST(PET_LIBS)
AC_SUBST(PET_BUILDDIR)
case "$with_pet" in
bundled)
	PET_CFLAGS="-I\$(top_srcdir)/pet/include"
	AC_DEFINE(HAVE_PET,[],[use Pet])
	;;
build)
	PET_BUILDDIR=`echo @abs_builddir@ | $with_pet_builddir/config.status --file=-`
	PET_CFLAGS="-I$pet_srcdir/include"
	AC_DEFINE(HAVE_PET,[],[use Pet])
	;;
system)
	PKG_CHECK_MODULES([PET], [pet])
	AC_DEFINE(HAVE_PET,[],[use Pet])
esac
AM_CONDITIONAL(BUNDLED_PET, test $with_pet = bundled)
AM_CONDITIONAL(BUILD_PET, test $with_pet = build)

AX_SUBMODULE(isl-polylib,build|bundled|system,bundled)

AC_SUBST(ISL_POLYLIB_CFLAGS)
AC_SUBST(ISL_POLYLIB_LIBS)
case "$with_isl_polylib" in
bundled)
	ISL_POLYLIB_CFLAGS="-I\$(top_srcdir)/isl-polylib/include -I\$(top_builddir)/isl-polylib/include"
	;;
build)
	ISL_POLYLIB_CFLAGS="-I$isl_polylib_srcdir/include -I$with_isl_polylib_builddir/include"
	ISL_POLYLIB_LIBS="$with_isl_polylib_builddir/libisl-polylib.la"
	;;
system)
	PKG_CHECK_MODULES([ISL_POLYLIB], [isl-polylib])
esac
AM_CONDITIONAL(BUNDLED_ISL_POLYLIB, test $with_isl_polylib = bundled)

AC_MSG_CHECKING(if the fractional representation should be used)
AC_ARG_ENABLE(fractional, [AS_HELP_STRING([--disable-fractional],
					  [Don't use fractional representation])],
	    [bv_cv_fractional=$enableval], [bv_cv_fractional="yes"])
AC_ARG_ENABLE(modulo, [AS_HELP_STRING([--disable-modulo],
				      [Don't use fractional representation])],
	    [bv_cv_modulo=$enableval], [bv_cv_modulo="$bv_cv_fractional"])
AC_MSG_RESULT($bv_cv_modulo)
if test "x$bv_cv_modulo" != "xno" ; then
        AC_DEFINE(USE_MODULO,[], [Use fractional representation])
fi

AC_MSG_CHECKING(whether to use incremental algorithm)
AC_ARG_ENABLE(incremental,
	[AS_HELP_STRING([--enable-incremental@<:@=bf|df@:>@],
			[Enable incremental algorithm [bf]])],
	[bv_cv_incremental=$enableval], [bv_cv_incremental="no"])
AC_MSG_RESULT($bv_cv_incremental)
case $bv_cv_incremental in
	yes | bf)
		AC_DEFINE(USE_INCREMENTAL_BF,[], 
			  [Use breadth-first incremental algorithm])
	;;
	df)
		AC_DEFINE(USE_INCREMENTAL_DF,[], 
			  [Use depth-first incremental algorithm])
	;;
esac

AC_ARG_WITH(ntl,
            [  --with-ntl=DIR          DIR Location of NTL],
            [ echo "Package ntl : $withval" && ntl_package=$withval],  
            [ ntl_package=yes ])

if test "$ntl_package" = "no"; then
    AC_MSG_ERROR(Need ntl)
fi

AC_SUBST(NTL_CPPFLAGS)
AC_SUBST(NTL_LDFLAGS)
if test "$ntl_package" != "yes"; then
    NTL_CPPFLAGS="-I$ntl_package/include"
    NTL_LDFLAGS="-L$ntl_package/lib"
fi

SAVE_CPPFLAGS="$CPPFLAGS"
SAVE_LDFLAGS="$LDFLAGS"
CPPFLAGS="$NTL_CPPFLAGS $CPPFLAGS"
LDFLAGS="$NTL_LDFLAGS $LDFLAGS"
AC_LANG_PUSH(C++)
AC_EGREP_CPP(yes, [
    #include <NTL/ZZ.h>
    #ifdef NTL_GMP_LIP
    yes
    #endif
    ],:,AC_MSG_ERROR(ntl not compiled with gmp support))
AC_LANG_POP

AC_CHECK_LIB(ntl, main,[],[
    AC_MSG_ERROR(Need ntl)
])
CPPFLAGS="$SAVE_CPPFLAGS"
LDFLAGS="$SAVE_LDFLAGS"

AC_SUBST(bv_cone_hilbert_basis)
AC_MSG_CHECKING(whether to compile zsolve)
AC_ARG_WITH(zsolve,
	    [AS_HELP_STRING([--without-zsolve],[do not compile zsolve])],
            [ with_zsolve=$withval],  [ with_zsolve=yes ])
AC_MSG_RESULT($with_zsolve)

use_zsolve=false
if test "$with_zsolve" != "no"; then
	if test "$GCC" = "yes"; then
		$CC --version |
		sed -e 's/.* \(@<:@0-9@:>@@<:@0-9@:>@*\)\.\(@<:@0-9@:>@@<:@0-9@:>@*\).*/\1 \2/' |
		(read major minor
			if test $major -gt 3; then
				exit 0
			fi
			if test $major -eq 3 -a $minor -ge 4; then
				exit 0
			fi
			exit 1
		) && use_zsolve=true
	fi
	if test "$use_zsolve" = false; then
		AC_MSG_WARN(gcc 3.4 required to compile zsolve)
	else
		AC_DEFINE(USE_ZSOLVE,[],[use zsolve])
		bv_cone_hilbert_basis="cone_hilbert_basis\$(EXEEXT)"
	fi
fi
AM_CONDITIONAL(USE_ZSOLVE, test x$use_zsolve = xtrue)

AX_SUBMODULE(omega,no|system,no)

AC_SUBST(OMEGA_CPPFLAGS)
AC_SUBST(OMEGA_LDFLAGS)
AC_SUBST(OMEGA_LIBS)
AC_SUBST(OMEGA_CODEGEN_LIBS)
have_omega=false
have_codegen=false
case "$with_omega" in
system)
    if test "x$with_omega_prefix" != "xyes"; then
	OMEGA_CPPFLAGS="-I$with_omega_prefix/include/omega"
	OMEGA_LDFLAGS="-L$with_omega_prefix/lib"
    else
	for i in /usr/include /usr/local/include; do
	    if test -f $i/omega/omega.h; then
		OMEGA_CPPFLAGS="-I$i/omega"
	    fi
	done
    fi

    AC_LANG_PUSH(C++)
    SAVE_CPPFLAGS="$CPPFLAGS"
    SAVE_LDFLAGS="$LDFLAGS"
    CPPFLAGS="$OMEGA_CPPFLAGS $CPPFLAGS"
    LDFLAGS="$OMEGA_LDFLAGS $LDFLAGS"
    AC_CHECK_HEADERS([omega.h],[
	OMEGA_LIBS="-lomega"
	AC_CHECK_LIB(code_gen,main,[
		OMEGA_CODEGEN_LIBS="-lcode_gen -lomega"
		have_codegen=true
	])
	have_omega=true
    ],[AC_MSG_WARN(Omega not found)])
    CPPFLAGS="$SAVE_CPPFLAGS"
    LDFLAGS="$SAVE_LDFLAGS"
    AC_LANG_POP
    ;;
esac
if test "$have_omega" = true; then
	AC_DEFINE(HAVE_OMEGA,[],[use omega])
fi
AM_CONDITIONAL(HAVE_OMEGA, test x$have_omega = xtrue)

use_parker=false
AC_MSG_CHECKING(whether to compile parker)
AC_ARG_WITH(parker,
	    [AS_HELP_STRING([--without-zsolve],[do not compile zsolve])],
            [ with_parker=$withval],  [ with_parker=yes ])
AC_MSG_RESULT($with_parker)

if test "$with_parker" != "no"; then
	AC_MSG_CHECKING(for mona)
	AC_ARG_WITH(mona,
		[AS_HELP_STRING([--with-mona=DIR],[mona *source* directory])],
				[], [AC_MSG_RESULT(not specified)])

	if test "x$with_mona" != "x"; then
		AC_SUBST(MONAPATH)
		MONAPATH=$with_mona
		AC_MSG_RESULT($MONAPATH)
		use_parker=true
		AC_DEFINE(USE_PARKER,[],[use parker])
	fi

	if test "x$with_mona" = "x"; then
		AC_MSG_WARN(mona source directory required to compile parker)
		AC_MSG_WARN(http://www.brics.dk/mona/download)
	fi
fi
AM_CONDITIONAL(USE_PARKER, test x$use_parker = xtrue)

AC_MSG_CHECKING(whether to use cddlib)
AC_ARG_WITH(cddlib, [AS_HELP_STRING([--with-cddlib=DIR],[DIR Location of cddlib])],
            [ cddlib_package=$withval],  [ cddlib_package=yes ])
AC_MSG_RESULT($cddlib_package)

have_cddlib=false
if test "$cddlib_package" != "no"; then
    if test "x$cddlib_package" != "xyes"; then
	CPPFLAGS="-I$cddlib_package/include $CPPFLAGS"
	LDFLAGS="-L$cddlib_package/lib $LDFLAGS"
    fi
    AC_TRY_LINK([
#define GMPRATIONAL
#include <setoper.h>
#include <cddmp.h>
], [
    mytype a;
    dd_init(a); ], [
	have_cddlib=true
	AC_CHECK_LIB(cddgmp, main,[
		SAVE_LIBS="$LIBS"
		LIBS="-lcddgmp $LIBS"
		AC_DEFINE_UNQUOTED([SRCDIR], ["$srcdir"], [srcdir])
		AC_LANG_PUSH(C++)
		AC_RUN_IFELSE([AC_LANG_PROGRAM([[#define GMPRATIONAL
#include <setoper.h>
#include <cdd.h>]],
		[[
	const char *filename = SRCDIR"/cdd94e-test";
	FILE *f;
	dd_MatrixPtr M;
	dd_LPType *lp;
	dd_ErrorType err = dd_NoError;

	dd_set_global_constants();

	f = fopen(filename, "r");
	M = dd_PolyFile2Matrix(f, &err);
	lp = dd_Matrix2LP(M, &err);
	dd_LPSolve(lp, dd_DualSimplex, &err);
	return lp->LPS == dd_Inconsistent;
		]])],[
			AC_DEFINE(HAVE_LIBCDDGMP, [], [Define to 1 if you have the `cddgmp' library (-lcddgmp).])
		],[
			AC_MSG_WARN(please upgrade cddlib to 0.94e or later)
			LIBS="$SAVE_LIBS"
			have_cddlib=false
		])
		AC_LANG_POP
	],[have_cddlib=false])
    ])
fi
AM_CONDITIONAL(HAVE_CDDLIB, test x$have_cddlib = xtrue)



AC_MSG_CHECKING(whether to use GLPK)
AC_ARG_WITH(glpk, [AS_HELP_STRING([--with-glpk=DIR],[DIR Location of GLPK])],
            [ glpk_package=$withval],  [ glpk_package=yes ])
AC_MSG_RESULT($glpk_package)

have_glpk=false
if test "$glpk_package" != "no"; then
    if test "x$glpk_package" != "xyes"; then
	CPPFLAGS="-I$glpk_package/include $CPPFLAGS"
	LDFLAGS="-L$glpk_package/lib $LDFLAGS"
    fi
    AC_CHECK_HEADERS([glpk.h],[
	have_glpk=true
	AC_CHECK_LIB(glpk, main,[],[have_glpk=false])
    ])
fi
AM_CONDITIONAL(HAVE_GLPK, test x$have_glpk = xtrue)

AC_MSG_CHECKING(location of TOPCOM)
AC_ARG_WITH(topcom, [AS_HELP_STRING([--with-topcom=DIR],[Location of TOPCOM])],
            [topcom_package=$withval], [topcom_package=unspecified])
AC_MSG_RESULT($topcom_package)
have_topcom=false
if test "$topcom_package" != no; then
	topcom_path="$PATH"
	if test "$topcom_package" != unspecified; then
		topcom_path="$topcom_package/bin"
	fi
	AC_PATH_PROG([POINTS2TRIANGS], [points2triangs], [], [$topcom_path])
	if test -n "$POINTS2TRIANGS"; then
		have_topcom=true
		AC_DEFINE_UNQUOTED(POINTS2TRIANGS_PATH, ["$POINTS2TRIANGS"],
				    [Path of points2triangs])
	fi
fi
AM_CONDITIONAL(HAVE_TOPCOM, test x$have_topcom = xtrue)

AX_SUBMODULE(cloog,no|build|bundled|system,bundled)

AC_SUBST(CLOOG_LDFLAGS)
AC_SUBST(CLOOG_CFLAGS)
AC_SUBST(CLOOG_LIBS)
CLOOG_CFLAGS="-DCLOOG_INT_GMP=1"
case "$with_cloog" in
bundled)
	CLOOG_CFLAGS="$CLOOG_CFLAGS -I\$(top_srcdir)/cloog/include -I\$(top_builddir)/cloog/include"
	AC_DEFINE(HAVE_CLOOG,[],[use CLooG])
	;;
build)
	with_cloog_builddir=`echo @abs_builddir@ | $with_cloog_builddir/config.status --file=-`
	CLOOG_CFLAGS="$CLOOG_CFLAGS -I$cloog_srcdir/include -I$with_cloog_builddir/include"
	CLOOG_LIBS="$with_cloog_builddir/libcloog-isl.la"
	AC_DEFINE(HAVE_CLOOG,[],[use CLooG])
	;;
system)
	if test "x$with_cloog_prefix" != "x"; then
		CLOOG_CFLAGS="$CLOOG_CFLAGS -I$with_cloog_prefix/include"
	fi
	if test "x$with_cloog_exec_prefix" != "x"; then
		CLOOG_LDFLAGS="-L$with_cloog_exec_prefix/lib"
	fi
	CLOOG_LIBS="-lcloog-isl"
	AC_DEFINE(HAVE_CLOOG,[],[use CLooG])
esac
AM_CONDITIONAL(BUNDLED_CLOOG, test $with_cloog = bundled)

AC_SUBST(GIT_HEAD_ID)
AC_SUBST(GIT_HEAD)
AC_SUBST(GIT_HEAD_VERSION)
if test -f $srcdir/.git/HEAD; then
    GIT_HEAD="$srcdir/.git/index"
    GIT_REPO="$srcdir/.git"
    GIT_HEAD_ID=`GIT_DIR=$GIT_REPO git describe`
elif test -f $srcdir/GIT_HEAD_ID; then
    GIT_HEAD_ID=`cat $srcdir/GIT_HEAD_ID`
else
    mysrcdir=`(cd $srcdir; pwd)`
    head=`basename $mysrcdir | sed -e 's/.*-//'`
    head2=`echo $head | sed -e 's/[^0-9a-f]//'`
    head3=`echo $head2 | sed -e 's/........................................//'`
    base=`basename $mysrcdir | sed -e 's/-.*//'`
    if test "x$head3" = "x" -a "x$head" = "x$head2"; then
	GIT_HEAD_ID="barvinok-$VERSION-$head
    elif test "x$base" = "xbarvinok-$VERSION" -a "x$head" != "x"; then
	GIT_HEAD_ID="barvinok-$VERSION-$head
    else
	GIT_HEAD_ID="UNKNOWN"
    fi
fi
if test -z "$GIT_REPO" ; then
    GIT_HEAD_VERSION="$GIT_HEAD_ID"
else
    GIT_HEAD_VERSION="\`GIT_DIR=$GIT_REPO git describe\`"
fi
echo '#define GIT_HEAD_ID "'$GIT_HEAD_ID'"' > version.h

AC_LANG_PUSH(C++)
AC_TRY_COMPILE([
		#include <set>
		void negate();
	], [negate()],
	suffix="normal", suffix="broken")
AC_CONFIG_COMMANDS([barvinok/set.h],
		   [cp $srcdir/barvinok/set.h.$suffix barvinok/set.h],
		   [suffix="$suffix"])
AC_CONFIG_COMMANDS([barvinok/NTL.h],
		   [cp $srcdir/barvinok/NTL.h.$suffix barvinok/NTL.h],
		   [suffix="$suffix"])
use_fdstream=false
SAVE_CPPFLAGS="$CPPFLAGS"
CPPFLAGS="-I$srcdir $CPPFLAGS"
AC_TRY_COMPILE([#include "fdstream.h"],[],
	[AC_DEFINE(USE_FDSTREAM,[],[use fdstream])
	use_fdstream=true])
CPPFLAGS="$SAVE_CPPFLAGS"
AC_LANG_POP
AM_CONDITIONAL(USE_FDSTREAM, [test "$use_fdstream" = true])

PACKAGE_CFLAGS="$PACKAGE_CFLAGS_ISL"
PACKAGE_LIBS="-lbarvinok $EXTRA_PACKAGE_LIBS -lisl-polylib -lpolylibgmp -lisl"
AX_CREATE_PKGCONFIG_INFO

AC_CONFIG_HEADERS(config.h)
AC_CONFIG_FILES(Makefile zsolve/Makefile doc/Makefile)
AC_CONFIG_FILES(parker/Makefile)
if test $with_polylib = bundled; then
	AC_CONFIG_SUBDIRS(polylib)
fi
if test $with_isl = bundled; then
	AC_CONFIG_SUBDIRS(isl)
fi
if test $with_pet = bundled; then
	AC_CONFIG_SUBDIRS(pet)
fi
if test $with_isl_polylib = bundled; then
	AC_CONFIG_SUBDIRS(isl-polylib)
fi
if test $with_cloog = bundled; then
	AC_CONFIG_SUBDIRS(cloog)
fi
AC_CONFIG_COMMANDS_POST([
	dnl pass on arguments to subdir configures, but don't
	dnl add them to config.status
	ac_configure_args="$ac_configure_args $bv_configure_args"
])
AC_OUTPUT
