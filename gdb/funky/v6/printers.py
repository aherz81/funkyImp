# Pretty-printers for libstdc++.

# Copyright (C) 2008-2015 Free Software Foundation, Inc.

# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

import gdb
import itertools
import re
import sys

### Python 2 + Python 3 compatibility code

# Resources about compatibility:
#
#  * <http://pythonhosted.org/six/>: Documentation of the "six" module

# FIXME: The handling of e.g. std::basic_string (at least on char)
# probably needs updating to work with Python 3's new string rules.
#
# In particular, Python 3 has a separate type (called byte) for
# bytestrings, and a special b"" syntax for the byte literals; the old
# str() type has been redefined to always store Unicode text.
#
# We probably can't do much about this until this GDB PR is addressed:
# <https://sourceware.org/bugzilla/show_bug.cgi?id=17138>

if sys.version_info[0] > 2:
    ### Python 3 stuff
    Iterator = object
    # Python 3 folds these into the normal functions.
    imap = map
    izip = zip
    # Also, int subsumes long
    long = int
else:
    ### Python 2 stuff
    class Iterator:
        """Compatibility mixin for iterators

        Instead of writing next() methods for iterators, write
        __next__() methods and use this mixin to make them work in
        Python 2 as well as Python 3.

        Idea stolen from the "six" documentation:
        <http://pythonhosted.org/six/#six.Iterator>
        """

        def next(self):
            return self.__next__()

    # In Python 2, we still need these from itertools
    from itertools import imap, izip

# Try to use the new-style pretty-printing if available.
_use_gdb_pp = True
try:
    import gdb.printing
except ImportError:
    _use_gdb_pp = False

# Try to install type-printers.
_use_type_printing = False
try:
    import gdb.types
    if hasattr(gdb.types, 'TypePrinter'):
        _use_type_printing = True
except ImportError:
    pass

# Starting with the type ORIG, search for the member type NAME.  This
# handles searching upward through superclasses.  This is needed to
# work around http://sourceware.org/bugzilla/show_bug.cgi?id=13615.
def find_type(orig, name):
    typ = orig.strip_typedefs()
    while True:
        search = str(typ) + '::' + name
        try:
            return gdb.lookup_type(search)
        except RuntimeError:
            pass
        # The type was not found, so try the superclass.  We only need
        # to check the first superclass, so we don't bother with
        # anything fancier here.
        field = typ.fields()[0]
        if not field.is_base_class:
            raise ValueError("Cannot find type %s::%s" % (str(orig), name))
        typ = field.type

class StdVectorPrinter:
    "Print a funky::LinearArray"

    class _iterator(Iterator):
        def __init__ (self, start, maxcount, bitvec):
            self.bitvec = bitvec
            self.item = start
            self.maxcount = maxcount
            self.count = 0

        def __iter__(self):
            return self

        def __next__(self):
#            raise StopIteration
            count = self.count
            self.count = self.count + 1
            if count >= self.maxcount:
                raise StopIteration
            elt = self.item.dereference()
            self.item = self.item + 1
            return ('[%d]' % count, elt)

    def __init__(self, typename, val):
        self.typename = typename
        self.val = val
        self.is_bool = 0 #val.type.template_argument(0).code  == gdb.TYPE_CODE_BOOL

    def children(self):
        try:
            return self._iterator(self.val['array']['items'],
                              self.val['COUNT'],
                              self.is_bool)
        except RuntimeError: 
            return self._iterator(self.val,
                              0,
                              self.is_bool)


    def to_string(self):
        start = self.val['array']['items']	
        count = self.val['COUNT']
        size = self.val['array']['SIZE']
        return ('%s of length %d, capacity %d'
                % (self.typename, int (count), int (size)))

    def display_hint(self):
        return 'array'

class ProjectionPrinter:
    "Print a funky::Projection"

    class _iterator(Iterator):
        def __init__ (self, start, maxcount, bitvec, offset):
            self.bitvec = bitvec
            self.item = start
            self.maxcount = maxcount
            self.count = -1
            self.offset = offset

        def __iter__(self):
            return self

        def __next__(self):
            count = self.count
            self.count = self.count + 1
            if count == -1:
                return ('offset', self.offset)
            if count >= self.maxcount:
                raise StopIteration
            elt = self.item.dereference()
            self.item = self.item + 1
            return ('[%d]' % (count), elt)

    def __init__(self, typename, val):
        self.typename = typename
        self.val = val
        self.is_bool = 0 #val.type.template_argument(0).code  == gdb.TYPE_CODE_BOOL

    def children(self):
#         return None
        return self._iterator(self.val['object']['array']['items'],
                              self.val['object']['COUNT'],
                              self.is_bool,self.val['offset'])

    def to_string(self):
        offset = self.val['offset']	
        start = self.val['object']['array']['items']	
        count = self.val['object']['COUNT']
        size = self.val['object']['array']['SIZE']
        return ('%s of length %d, offset %d, capacity %d'
                % (self.typename, int (count), int (offset), int (size)))

    def display_hint(self):
        return 'array'


#class StdVectorIteratorPrinter:
#    "Print std::vector::iterator"
#
#    def __init__(self, typename, val):
#        self.val = val
#
#    def to_string(self):
#        return self.val['_M_current'].dereference()


class SingleObjContainerPrinter(object):
    "Base class for printers of containers of single objects"

    def __init__ (self, val, viz):
        self.contained_value = val
        self.visualizer = viz

    def _recognize(self, type):
        """Return TYPE as a string after applying type printers"""
        global _use_type_printing
        if not _use_type_printing:
            return str(type)
        return gdb.types.apply_type_recognizers(gdb.types.get_type_recognizers(),
                                                type) or str(type)

    class _contained(Iterator):
        def __init__ (self, val):
            self.val = val

        def __iter__ (self):
            return self

        def __next__(self):
            if self.val is None:
                raise StopIteration
            retval = self.val
            self.val = None
            return ('[contained value]', retval)

    def children (self):
        if self.contained_value is None:
            return self._contained (None)
        if hasattr (self.visualizer, 'children'):
            return self.visualizer.children ()
        return self._contained (self.contained_value)

    def display_hint (self):
        # if contained value is a map we want to display in the same way
        if hasattr (self.visualizer, 'children') and hasattr (self.visualizer, 'display_hint'):
            return self.visualizer.display_hint ()
        return None

# A "regular expression" printer which conforms to the
# "SubPrettyPrinter" protocol from gdb.printing.
class RxPrinter(object):
    def __init__(self, name, function):
        super(RxPrinter, self).__init__()
        self.name = name
        self.function = function
        self.enabled = True

    def invoke(self, value):
        if not self.enabled:
            return None

        if value.type.code == gdb.TYPE_CODE_REF:
            if hasattr(gdb.Value,"referenced_value"):
                value = value.referenced_value()

        return self.function(self.name, value)

# A pretty-printer that conforms to the "PrettyPrinter" protocol from
# gdb.printing.  It can also be used directly as an old-style printer.
class Printer(object):
    def __init__(self, name):
        super(Printer, self).__init__()
        self.name = name
        self.subprinters = []
        self.lookup = {}
        self.enabled = True
        self.compiled_rx = re.compile('^([a-zA-Z0-9_:]+)(<.*>)?(::Version)?$')

    def add(self, name, function):
        # A small sanity check.
        # FIXME
        #if not self.compiled_rx.match(name):
        #    raise ValueError('funky programming error: "%s" does not match' % name)
        printer = RxPrinter(name, function)
        self.subprinters.append(printer)
        self.lookup[name] = printer

    # Add a name using _GLIBCXX_BEGIN_NAMESPACE_VERSION.
    def add_version(self, base, name, function):
        self.add(base + name, function)
        self.add(base + '__7::' + name, function)

    # Add a name using _GLIBCXX_BEGIN_NAMESPACE_CONTAINER.
    def add_container(self, base, name, function):
        self.add_version(base, name, function)
        self.add_version(base + '__cxx1998::', name, function)

    @staticmethod
    def get_basic_type(type):
        # If it points to a reference, get the reference.
        if type.code == gdb.TYPE_CODE_REF:
            type = type.target ()

        # Get the unqualified type, stripped of typedefs.
        type = type.unqualified ().strip_typedefs ()

        return type.tag

    def __call__(self, val):
        type = val.type
        if val.type.code == gdb.TYPE_CODE_PTR:
            type = val.type.target()

        typename = self.get_basic_type(type)

        if not typename:
            return None

        # All the types we match are template types, so we can use a
        # dictionary.
        match = self.compiled_rx.match(typename)
        if not match:
            return None

        basename = match.group(1)

        if val.type.code == gdb.TYPE_CODE_REF:
            if hasattr(gdb.Value,"referenced_value"):
                val = val.referenced_value()
        if val.type.code == gdb.TYPE_CODE_PTR:
            try:
                val = val.dereference()                
            except RuntimeError: 
                return None            

        if basename in self.lookup:
            return self.lookup[basename].invoke(val)

        # Cannot find a pretty printer.  Return None.
        return None

funky_printer = None

class TemplateTypePrinter(object):
    r"""A type printer for class templates.

    Recognizes type names that match a regular expression.
    Replaces them with a formatted string which can use replacement field
    {N} to refer to the \N subgroup of the regex match.
    Type printers are recusively applied to the subgroups.

    This allows recognizing e.g. "std::vector<(.*), std::allocator<\\1> >"
    and replacing it with "std::vector<{1}>", omitting the template argument
    that uses the default type.
    """

    def __init__(self, name, pattern, subst):
        self.name = name
        self.pattern = re.compile(pattern)
        self.subst = subst
        self.enabled = True

    class _recognizer(object):
        def __init__(self, pattern, subst):
            self.pattern = pattern
            self.subst = subst
            self.type_obj = None

        def recognize(self, type_obj):
            if type_obj.tag is None:
                return None

            m = self.pattern.match(type_obj.tag)
            if m:
                subs = list(m.groups())
                for i, sub in enumerate(subs):
                    if ('{%d}' % (i+1)) in self.subst:
                        # apply recognizers to subgroup
                        rep = gdb.types.apply_type_recognizers(
                                gdb.types.get_type_recognizers(),
                                gdb.lookup_type(sub))
                        if rep:
                            subs[i] = rep
                subs = [None] + subs
                return self.subst.format(*subs)
            return None

    def instantiate(self):
        return self._recognizer(self.pattern, self.subst)

def add_one_template_type_printer(obj, name, match, subst):
    printer = TemplateTypePrinter(name, '^std::' + match + '$', 'std::' + subst)
    gdb.types.register_type_printer(obj, printer)

class FilteringTypePrinter(object):
    def __init__(self, match, name):
        self.match = match
        self.name = name
        self.enabled = True

    class _recognizer(object):
        def __init__(self, match, name):
            self.match = match
            self.name = name
            self.type_obj = None

        def recognize(self, type_obj):
            if type_obj.tag is None:
                return None

            if self.type_obj is None:
                if not self.match in type_obj.tag:
                    # Filter didn't match.
                    return None
                try:
                    self.type_obj = gdb.lookup_type(self.name).strip_typedefs()
                except:
                    pass
            if self.type_obj == type_obj:
                return self.name
            return None

    def instantiate(self):
        return self._recognizer(self.match, self.name)

def add_one_type_printer(obj, match, name):
    printer = FilteringTypePrinter(match, 'std::' + name)
    gdb.types.register_type_printer(obj, printer)

def register_type_printers(obj):
    global _use_type_printing

    return


def register_funky_printers (obj):
    "Register funkyImp pretty-printers with objfile Obj."

    global _use_gdb_pp
    global funky_printer

    if _use_gdb_pp:
        gdb.printing.register_pretty_printer(obj, funky_printer)
    else:
        if obj is None:
            obj = gdb
        obj.pretty_printers.append(funky_printer)

    register_type_printers(obj)

def build_funky_dictionary ():
    global funky_printer

    funky_printer = Printer("funky-v6")

    # For _GLIBCXX_BEGIN_NAMESPACE_VERSION.
    vers = '(__7::)?'
    # For _GLIBCXX_BEGIN_NAMESPACE_CONTAINER.
    container = '(__cxx1998::' + vers + ')?'

    # libstdc++ objects requiring pretty-printing.
    # In order from:
    # http://gcc.gnu.org/onlinedocs/libstdc++/latest-doxygen/a01847.html
    funky_printer.add_container('funky::', 'LinearArray', StdVectorPrinter)
    funky_printer.add_container('funky::', 'Projection', ProjectionPrinter)
    # vector<bool>

    # Printer registrations for classes compiled with -D_GLIBCXX_DEBUG.
    funky_printer.add('funky::__debug::LinearArray', StdVectorPrinter)
    funky_printer.add('funky::__debug::Projection', ProjectionPrinter)

    # These are the TR1 and C++0x printers.
    # For array - the default GDB pretty-printer seems reasonable.

    # These are the C++0x printer registrations for -D_GLIBCXX_DEBUG cases.
    # The tr1 namespace printers do not seem to have any debug
    # equivalents, so do no register them.

    # Library Fundamentals TS components

    #if True:
        # These shouldn't be necessary, if GDB "print *i" worked.
        # But it often doesn't, so here they are.
        #funky_printer.add_version('__gnu_cxx::', '__normal_iterator',
        #                              StdVectorIteratorPrinter)

        # Debug (compiled with -D_GLIBCXX_DEBUG) printer
        # registrations.  The Rb_tree debug iterator when unwrapped
        # from the encapsulating __gnu_debug::_Safe_iterator does not
        # have the __norm namespace. Just use the existing printer
        # registration for that.

build_funky_dictionary ()
