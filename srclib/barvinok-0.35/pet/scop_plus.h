#ifndef PET_SCOP_PLUS_H
#define PET_SCOP_PLUS_H

#include <set>
#include <clang/AST/Decl.h>

#include "scop.h"

void pet_scop_collect_arrays(struct pet_scop *scop,
				std::set<clang::ValueDecl *> &arrays);

#endif
