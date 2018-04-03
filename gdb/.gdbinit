python
#.gdbinit file to add pretty printing for funky arrays (rename to .gdbinit and place in your home folder after fixing paths)
#assumes funky was checked out into ~/svn/funkyimp
#works with gdb 7.8 (and netbeans 8 when funky files use the .f extension and the c++ plugin)
import sys 
sys.path.insert(0, '~/svn/funkyimp/gdb')
from funky.v6.printers import register_funky_printers
end
