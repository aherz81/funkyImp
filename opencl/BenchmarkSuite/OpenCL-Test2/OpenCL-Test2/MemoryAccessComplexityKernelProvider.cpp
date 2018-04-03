//
//  MemoryAccessComplexityKernelProvider.cpp
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 06.05.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#include "MemoryAccessComplexityKernelProvider.h"



//
//  MultipleBasicOperationsKernelProvider.cpp
//  OpenCL-Test2
//
//  Created by Alexander Pöppl on 02.04.14.
//  Copyright (c) 2014 Alexander Pöppl. All rights reserved.
//

#include "MultipleBasicOperationsKernelProvider.h"

#include <memory>
#include <sstream>

using namespace std;


static const size_t benchmarkMemSize = 256*1024;

//RAW STRING
static const string kernelString = R"LIM(
__kernel void wg_kernel(global float *memory) {
    int work_item = get_global_id(0);
    )LIM";
//END

static const string xDef = "int x = (work_item>>11) & 2047;\n\t";
static const string yDef = "int y = work_item & 2047;\n\t";

//RAW STRING
static const string kernelContinuation = R"LIM(
    memory[work_item] = memory[()LIM";
//END
                               
                               
//RAW STRING
static const string kernelEnd = R"LIM(
                               )&262143];
})LIM";
//END

//GENERATED CODE
vector<vector<string>> memoryAccesses = {
	{"885", "y", "y", "225", "125", "913", "129", "558", "x", "x", "x", "601", "x", "y", "y", "294", "418", "y", "302", "512", },
	{"1012 + x", "x + 248", "569 * y", "y * 173", "y * y", "x + x", "x + 944", "y * 896", "473 + x", "y + x", "y + 464", "y + 218", "203 + x", "y + 12", "x * x", "822 + y", "y * x", "x * x", "551980 * y", "x + 231", },
	{"y * (984 + y)", "741 * (976 * x)", "(y * y) * 352", "(35 + x) * 1790472192", "(x * y) + x", "258 + (y + y)", "x + (602 * y)", "(y + 718) + 615", "y * (y + x)", "(x + 50317) + x", "x + (959 + x)", "(x + 566) * y", "686 + (y * x)", "(y * 355) + 196", "x + (1097 + y)", "y * (y * 59772)", "683 + (y * 309)", "83 * (y + x)", "(y + 721) + y", "x + (1557 * y)", },
	{"((y * x) + 351) * x", "247 + (669 + (866 * x))", "(y * (y * y)) + 805043200", "853 * ((y + 204) * 983)", "((y * 660) * x) * y", "(y * (10 * y)) * x", "(751 * (x + x)) + y", "(y + y) * (659 * x)", "((1021 + x) + y) * x", "((x + x) + 891) * 716", "(y + 681) + (y + 115)", "(y + 381) * (461 * x)", "(y + (x + y)) * 537", "160 + (1011 + (141 * y))", "(y * x) + (y * y)", "533 + ((x * 798) * 784)", "y * (y * (x * x))", "y * ((x + 725) + 944)", "x * ((139 * x) * x)", "((x + 348) + x) + x", },
	{"(817 * ((988 + y) * x)) + 1019", "x + (12 + (223967172 + (x + x)))", "(x * (y + (607 + y))) * x", "((579 + (x + y)) * 951) * 6", "(x * x) + ((309 * x) * 557)", "519 + (536 + (687 * (y * 119)))", "((y * y) + y) * (126 * y)", "(x + (131 + y)) + (y + 860)", "((911 * (310 + y)) + 463) + x", "(854 + x) * (345 * (y * x))", "(370335042 + (x * 73981)) * (109 * x)", "(68096 * x) + ((y + 600) * x)", "x * ((137 + (44 * x)) * y)", "(873 + (y * (y * 610))) * 113", "((710 * (y * 399)) * y) + x", "x * (x * (250462224 * (x + 442)))", "((1335 + (y + 557)) * 176) + 267", "478 + ((36 + (y + x)) * x)", "(y + 88) * ((317 * x) + y)", "(((y * y) + 9) * 152) * 213", },
	{"((x * x) * (334 * (y * y))) + 594", "(x + 314) + (306 + (431 * (x + y)))", "(x * (x * ((x + y) + 37))) + x", "(506 * (1082 * (x * y))) * (y + 958)", "(((851 * (x * 873)) + 37) * y) * 969", "x * ((312 + y) * ((x + 994) + y))", "((y * (x + x)) * 784) + (207 + x)", "((y + 134) * y) * (y + (y * y))", "853 * ((y + (10 + (y + 968))) + 1247)", "(x * (x + (y * (y * 286)))) * 679428", "((520 * ((y + x) * y)) + 691) + 712", "y * (973 * (((y + x) + x) * x))", "(x * (807 * (860 * (32 * y)))) * x", "(y * y) * (((y * 77) * y) + 441)", "367 * (((52 + x) + (x * y)) + x)", "((y * (y + (277 * y))) + 307) + 793", "967 * (((y * 893) * (x * x)) * 174)", "(((y * 480) + x) + (x * x)) * 421", "(((x * 417) * 318250) * (236 + x)) * 674", "x + ((511 * ((383 * y) + x)) * y)", },
	{"(x * (x + (449 + y))) * ((y + 107) + y)", "(525 * (505 + (((x + 447) + x) + x))) + y", "((y * 538) * 425) + ((779 + (y * 959)) + 380)", "(941 + (x + (x + 924426))) * ((y * 524112) * x)", "(y * y) * ((((534 + x) * 890) + 573) * y)", "588 + (x * (318052 * (y * (64757 + (172189 + x)))))", "((722 + x) + 598) * ((361 * (x + 79)) * 633)", "169 * (693 * (((y + (543 + x)) * x) + 101))", "(((y * x) + 903) + (519 + (x + y))) + 269", "958 * ((792 + (576 + (962 + x))) + (x + 915))", "194 + ((402 * y) * ((x + (y + x)) + 409))", "(x * (x + ((x + x) * (615 * y)))) + 860", "(y * (y + (859 * (x * (855 * x))))) * x", "((y + x) * y) + (y + ((x * y) + 11))", "299 + ((166 + ((569 + (x * y)) * 302940)) + 676)", "x * (y + ((x * (y * 135)) + (x * y)))", "x * (((x + (x * x)) * (848 + y)) + 30)", "(853 + ((y * 243) * ((x + x) * y))) + 757", "(21 + (x + (381 * (x + x)))) * (y * 237)", "(y + 994) + ((13209385 + y) + (x * (1836 + y)))", },
	{"((443 * ((x * x) + (x * x))) * (y + y)) * 407", "(((((y * 58) + y) + y) * (x * 849)) + x) * 535", "((y + 715) + 369240) * (446 + (x * ((y + y) * 1357)))", "(973 * (y + (866 + (318 + ((147480 + y) + 324))))) + 283", "x + (((y * (x + (301 + x))) + 832053) + (x + y))", "(y * (x * ((246 + (175 * y)) + (x + x)))) + x", "710 * ((y + y) + (((x + (x + 553)) + 264) + 78))", "x * (y * (((((695 + x) + 495) * 224) + x) * 103))", "(x * ((304 * (y + 1017)) + x)) * ((x + 966) * x)", "(((((y * 1003) + y) * 577596) + (39 + x)) + 936) + 31", "64050 + (x * ((62 * (((y * x) * y) + 178104)) + 189280))", "527 * (1073 + (y * (8 + (((x * 120) + y) * 1011))))", "149 + (((y * (731 + (x + 772))) + (x * y)) * 217)", "y + ((774 * (91 * (y * 841))) + ((y + x) + 770))", "((x * 798) * ((x * x) + x)) + ((660 * x) * 1307)", "y * ((332 + (49 * y)) * (959 + ((75544 * x) * x)))", "119 * (((y + ((x + y) + y)) + (y + 10592)) * 72224)", "((x * x) * (572 + ((y + x) + (549 + y)))) * 458", "165 + ((((x * ((825 + x) + 702758)) * y) + y) * x)", "((298 + (x * y)) + 931) + ((1010 + y) + (y * y))", },
	{"((446 + (1136 * (x * (276 * x)))) + ((y + 823) * y)) + y", "((152 * ((x * y) * ((451903 + x) * 641))) * (880 + x)) + y", "((((893 + x) * (x + y)) + y) + y) * (842 * (x + x))", "(314 + ((((x + (y + 11)) + 373) + 11) * (152607 + x))) * 38", "y * (x + (82 + (y * ((240 * y) + ((548 * y) + 950)))))", "(663 + ((((55 * (960 * (y + x))) * 177) + -786590432) * y)) + 547", "(482 + x) * (215 * (y * (x + (y * ((x + 858) * 1626)))))", "((((727 + (1716 + (481 + y))) * 515) + y) * (188 + x)) + 930", "174 * ((x * ((((966 + x) * y) * 873) * (504 * y))) + 822)", "y + (y * ((((x * x) * (y * y)) + (y + 350)) + 414))", "((276 * (933 * x)) * ((x * x) * y)) * ((36 * x) * 1525)", "((((x + y) + 23) + 10) + 905) + ((5976 + (x + x)) * 41)", "(x * ((x + 1332) + 207)) + ((x * (y * (x + 232))) * 183)", "554 * ((((1006 * ((x * 1004) * 410)) + y) + 999) + (x + 236))", "(462 * ((121 * ((y + y) * (y + x))) + (x * x))) + 898", "x + ((((((y * 724) + (y * y)) * 630064) * y) + 249) + 185)", "(y + ((69160 + ((836 * y) * (365 + x))) + y)) + (x + y)", "(y * 238) + (((x + (y + (y + (350 + x)))) * 81) + y)", "(y * 286) + (((x + x) * (x + 771)) * (511 + (y + 767)))", "x + (((y + ((y * 679) * (x + (y + 25898)))) * x) + x)", },
	{"458 + (877 + (x * ((y * x) * (x * (66 * ((381 * x) + x))))))", "y * (y + (732 * (236575 * (((x + 176) + (x + (195 * x))) * y))))", "(y * (((x + 1015) * y) + 633)) * ((((y * 410) * 182) + 823) + 53)", "x * (((354 * (900 + (x + 900))) * (((y + 515) * y) + x)) + 307)", "(((x + (x * ((933 + x) * (x + x)))) * (x + x)) * 794) + x", "725 + (960 * ((749 * (y + (((x + x) * 254) + 537))) + (x * 256)))", "(834 * (4 + (x + (x * 583)))) + ((900 * (828 * x)) + (x * 650))", "700 + ((((59 * (105984 + (997 * ((y + 972) + 487)))) + y) + 794) * 530)", "y * ((((((921 * y) + y) + ((770 * y) * 42506282)) * y) * 775) * 326040)", "(((x * (443 * (447 * y))) * (x * 264)) * y) * (x + (631 * y))", "((((((612 * x) * (y * (y + 709))) * y) + x) * y) + x) + 631", "(((x + (((880 * ((13 + y) * 350)) * 374) + x)) * y) + 413) + y", "(97710288 * y) + (((963 * ((116 + (672 * y)) + (y + 614))) + 901) + 905)", "y + ((x + 576) * ((888 * y) + ((937 * (129 * (x + 449))) + 650)))", "((388 * x) * (x + (((x + (y + x)) + (x * y)) + 684))) * 959", "y + (((664 + (450 + (856314 + x))) + (x * (y + 749))) * (962 + y))", "(x + (415397 + x)) * (785 * ((x + 705) * (y * ((x + 95) * x))))", "(x * (y * 1005)) + ((((x + x) + x) * ((x + y) * 295036)) * 837)", "((169 + ((x * x) + (241 + (x + y)))) * 658) + (y + (y * x))", "y + (((449895432 + (((x + x) + x) * ((x + 767) + 377))) * 819) + 652815)", },
	{"(((((577 * x) * ((48 * (454 + (x * x))) + y)) * 116) + x) * 584) + y", "(((((x * x) * y) + 748) * (430 + x)) + (y + (563 * (x * 636)))) * x", "(675 * ((((252 + (y + (((y + 709) * 328) + y))) + 560) + y) * 844)) + 47376", "x * ((y * (x + y)) * ((435 + (x * (((304 * x) * 167) + y))) * 466))", "(((516 * x) + (y * 112)) + ((x + 948) + 96)) * (x + ((y * x) + 622))", "(y * ((y * y) + (((x + y) + ((y * 237) + y)) + (y + 435)))) * x", "x * ((x + 381) + (x * (y * (539 * (223 + (273 + ((y + 255602) * 375725)))))))", "153 * (((503 * y) + (805 + ((x + (y + y)) * x))) + ((618 * x) + 401))", "(y * ((((704 * x) + (((409 + y) * x) * x)) * 54) + 50)) + (716 * x)", "381 + (((y * x) * ((x + -1541711368) + (y + (x + ((y + y) + x))))) + x)", "632 + (((92 + ((x + y) * y)) * 777650) + (715 * ((y * 229059) * (y + 321))))", "(((y + y) + x) * (458 + ((x * ((576 * x) * (446 * y))) + y))) * 982", "((x + ((y * ((y + 172) + (1003 * (529 + y)))) + (372 + y))) * y) + y", "((((280607094 * (y + (64 * (430 + (781 * y))))) * y) + (156 * y)) + 269) + x", "((((y + 12) * 664) * 200) * x) + ((((523 * y) + x) + y) + (x + 190))", "y * ((x + (((50 * y) * y) * y)) * (y * ((x * (732 + x)) * 252)))", "x + (((((967 * y) + ((77 + x) + ((y * 561) + 16))) + 44) * 241) + y)", "x + (((((328 + y) + x) * x) + (103 + (((351 * y) * y) + y))) + 37864)", "((689 + x) * 392) + ((y + ((x * y) * (y + x))) * ((x * 488) + 459))", "((((x * x) * (y + x)) + ((x * (924 * (x * 152))) + x)) * y) * 829", },
	{"y + ((((((598 + y) * x) + 25) + x) * (((x + (57279 + y)) * 481) + 63)) * y)", "((78 * (((y * (1640 + ((99 + y) + 1579))) + ((y + 343) + 211)) * x)) * 282) * 279", "((((y + (637 * x)) + 271) + ((y + x) * 668)) * x) + ((x + (217 * y)) + x)", "(y + (((y + ((351 * (y + 13)) * x)) * ((312 + y) * y)) * 627)) + (x + 116604)", "((y * (x + ((258050 * (((y + 670) * ((y * x) + x)) * x)) + x))) * 233) * 201", "866 + (608 * ((728 + x) + ((777 * ((y * (69 + (-1908045698 + y))) + 669)) + (x * y))))", "(((y * (2661 + ((319 + (y + (379 + (x * (y + x))))) + 365))) + y) * 422499) + 970", "((711 + x) + (869 * x)) * ((882 * ((1009 + x) * x)) * (904 + ((578 * y) * 317)))", "(x * (((((794 + ((505 * y) + (y + 1039))) * 154) + 794) + (553 + y)) * 549)) + 789", "y * ((y * (x + ((x + y) + x))) * (((x + (795 * (y + x))) * 66) + 410063))", "(y * (x + 141)) + ((69 * x) * (x * (607 * (((1017 + x) + y) * (y * x)))))", "y * ((449 * (999 + (660 + y))) * (((x + (x * x)) + ((46 * y) * 362)) * y))", "((((579 + (965 * x)) + y) * x) + (((214 + (x * x)) * x) * (y + 781))) + y", "710 * (((x * ((y + ((y + 548) + ((1020 * y) + y))) * (766100 + x))) * 918) + x)", "((1010 + (((((x + y) + 86) + y) + x) + y)) + (x * x)) * ((y + 982) * 360)", "(219 * (x + (354 + y))) * (529 + ((30629 + ((309 + y) * 762)) + (y * (217 * y))))", "(458 + (((((y + y) + y) + ((68400 + y) + x)) * (65 * (758 + y))) * 800)) + 578", "y * (952 + ((y * (794 + (x + y))) + (x * (y + ((394 * (y + y)) + 702)))))", "(y * x) * ((((y * ((x * ((237 + (x * x)) + 746)) * 33)) * 728) + x) + y)", "(((866 * y) * (((451655 * ((y + y) + 107)) * 215) + 256)) + ((853 * y) + y)) * 11", },
	{"(101 * y) * ((383 + (((y * 59) + y) + (((898 + y) * y) + ((y + 755) + x)))) * 455561)", "((1001 * y) + (y + x)) + ((((y * 263) + (((446 + y) * (548 + y)) * 827)) + 110) + 769)", "y + (((y + (1191 * (((((y * y) + 227) * (x * y)) * 335) + 1535))) * 366) + (x + 1511))", "(900 * ((x * ((577 * (((x * 794) + 540) + (182 * ((x * y) + x)))) + y)) + y)) + y", "((552 + (y + ((y * 4) * (y + (958 + (417 * (650 + y))))))) + 464) + (967 * (x + x))", "(972 + (814 * ((y + 583) + (982 * (y * (x + (x * 980))))))) * (x + (y * (752 * y)))", "967 * (y + (((y * (y * x)) * 782) * (((y + y) * (549861 + (602 * (330 * y)))) + x)))", "(549 + y) * ((x * (y + (x * x))) + (y * (((y * 546) * y) + ((831 + y) * 1204))))", "((((y + y) + (35 + ((897 * ((((y * 876) + x) + 810) + 457)) + x))) + x) * 793) + x", "x + ((817 * ((y + ((x * ((y * 44465) * 460)) + (y + y))) * 244)) * (y * (x + 279)))", "(y + ((((y + ((y + 1615) * x)) * (x * ((y + 208) * 17004))) + 370856) + (x * 895))) * y", "(x + (y + (87 + (x * (x * -277653515))))) + (507 * ((2336 + ((165 + x) + 481)) + (x + y)))", "(((439 * ((x * (((((348300 * y) + 871) * 835) + (y * 407)) + y)) * x)) + y) * x) * y", "347 + ((((x + x) * ((((526 * y) + 759) * 93) * y)) * ((252538 * (x + x)) + y)) * x)", "((((y * (y + ((x * (721 * (x + y))) + ((y + 951) + 436)))) + x) * y) * y) * 778", "(147 * (((((3359400 * (((x * y) + 46872) + (374 + y))) + 566) + (818 + x)) * 1624) + y)) * 829", "418605 * ((y + (x + ((y * y) + (623 + (y * y))))) * (x + (((x + 135) + 363) + x)))", "((673 * x) + y) + (307 + (((x + (1012 + (x + x))) * (928 * ((y + y) * 663))) * 140))", "516456 + (x + ((((823 * y) * 713) + ((682 + (y + x)) + (342 + ((y * 410) + x)))) * 520))", "((((((x * y) + x) + 90) * y) + (x + (939 + (y * (x + 265))))) + (628 * x)) + x", },
	{"(58 * y) * (((466 * (y * ((y + (y + y)) * (((x * y) * (989 + x)) + 192)))) * 781) * 533)", "(((((y * (y + ((x + x) + (y + y)))) * ((x * 167) + y)) * y) + 98) * x) * (374 + x)", "x * ((x + 322) * (((x + (x * 386)) * (((y * (((x * x) + x) + y)) + y) + 733)) + y))", "((((x + 513) * x) * ((937 + (((y + x) * x) * 745)) * (x + y))) + ((x * 230) + y)) + y", "(((x + (((x * 45) + 872) + ((((y + y) * x) + (377 + y)) + (444 * x)))) * x) * x) * 206", "932 + ((((x * ((690 + ((941 + (26000 + x)) * 649)) + y)) + (((x + 983) * 882) + y)) * x) * 893)", "(((755 * (x * (963 * y))) + (410 * ((y + x) + x))) + ((x * (x + 28)) + (y + x))) * 428", "x * ((y * ((((y * 453) + (y * ((y + x) * x))) + (x + ((y * x) * 762))) + 491)) * 620)", "(y * (726 * ((651 * y) * ((y * (299 + ((349 + (x + 55)) * y))) * 467)))) + (322 * (433 + y))", "387 + ((((x * ((486 + (y * 1205)) * (y + y))) + ((((1276 + x) * 286) * y) + 943)) * 892) * 648)", "y * (607620 + ((((755 * (x * x)) + 917) * ((((x + 905) + (x * x)) * (1019 + x)) * 29)) * x))", "((((((y + y) * x) + y) * x) * (18864 * ((y + x) + ((x + (264 * x)) * y)))) + y) + 6724080", "279 + ((((x * (y + 523)) * (y + ((((1364 + (x * 499)) + x) * x) * 920))) * (y * 985)) * x)", "(965 * ((x + 1004) * (((y + ((y + 328276) * 682)) + y) + 481))) + (x + (((y * 516) * 396) * x))", "(y + ((y + y) * 812)) * ((((x * (x * 722)) + y) * (((y * 369) + 680) + (x * y))) * 523)", "(((((x + x) * y) * (199716 + ((564 * (935 + (502 + y))) * y))) + y) * (899 * x)) * (x + 644)", "(y + (658 * (((183 + x) * y) + 136))) * (((37 * y) * x) + (((y + (147 + y)) * 614) * y))", "y + ((795 * ((54 + ((157 * y) * 1152)) * 899)) + ((295 + (x * ((y * (764 * x)) + 692))) * 982))", "(((1377 + y) * (y * x)) + 576) + (((x + (y * (113 + (606 + y)))) * 254) + ((727 * x) + 132))", "(((y + y) + (567 * (((x * ((x * 783) + (337 + (848946 + x)))) * x) + 249))) * y) + (x * 742)", },
	{"((277857 * (y * (564 * (300 + x)))) * y) * (((y * 859) * (((y + (724 + x)) * 227) + (578 * x))) + 139)", "y + (((y + x) * (x + x)) + (x + (x * ((171 * ((x + y) + 647)) * ((402 * x) * (y + 634))))))", "(((330 * (y + (x + x))) + ((671 + (((x * 857) * (159 + y)) * (895 * y))) + (944 * x))) * y) + y", "(793 * x) * (((y + (x * 229)) * ((781 + (((y * (255 + y)) + 425) + ((x + 282) * 110))) * 971)) * x)", "(232 + ((677 + y) * (((x + x) + y) * y))) + ((503 + ((x * ((194 * y) * (x + 661))) + 60)) * y)", "676 * ((x * y) * ((533 + (484 + (((844 * ((y + x) * x)) + 241) * (((y * 672) + x) + x)))) * 59))", "891 + (y + ((y + (x * ((y * y) * (y + x)))) * ((((x + x) * 247) + x) * (x * (x + x)))))", "(((44 + (683 * (353547 * x))) * y) + (y * x)) + ((1023 + ((x * x) * (((x + x) * 812) * y))) * x)", "79 * (x + ((637 + ((((x + ((y + ((204 * x) + x)) * (x * x))) * y) * x) * (x + x))) * 108403875))", "(((((y * ((x + x) + 657)) + ((y + 395) + x)) * x) * ((y * 795) * (y * y))) + y) * (1321 * y)", "((y + (((((594 + y) + ((766 * (y * y)) * (312 + x))) * x) * (1674 * (x + y))) * 400)) * x) * y", "(((((y + ((896 + y) * (y * (1164 * (y + 938))))) * x) * (x + 483)) * ((193 + x) * y)) * x) + x", "((((298 * y) + 436) * (581 * (y + (((((y * 352032) * y) + ((y + y) * 38)) + x) + y)))) + 196) + 1501903776", "(((((649 * x) * (y * 595)) + y) + (((y + y) * 692) + (874 + ((968 + y) * (x + 751))))) + 799) * 729", "(((x + ((x + ((y * 839) * x)) * (559 * x))) * 474) + 337) + ((x * (913 * (y * x))) + (x + 472))", "744 * ((((y * 276) * (y + 861)) * (((992 + y) + y) * ((850 + y) * (964 * (y * (y * x)))))) * 882)", "(y + 64) + (y + (((((602 * y) + (986 * (y + (y * (341 + ((y * 635) * 887)))))) * 847) * x) * x))", "(624 + ((x + (y + y)) * ((519 * x) * (980 * y)))) * ((1103 * ((x + ((699 * x) + 173)) * y)) * y)", "429 + ((x + y) * ((((x * y) + (x * ((y + y) + 107))) + (471 * y)) * ((x * x) * (x * 705))))", "-647638738 * (y * ((847 + x) * ((x + y) * (((x * (((((426 + y) * 654) + y) + 714) + x)) + 445) * y))))", },
	{"314 * (((x * ((773 * x) + x)) + y) * (y + ((335 + (135 + (x + (x * (294 + (945 + (x + 214))))))) + x)))", "(((466050 * ((y * (y * 518)) * (((y * ((x + x) + 1618)) + ((447 + (431 * y)) + x)) + y))) * 773) * x) * x", "(((((713 * ((y + 926) + y)) * 945) * 561) * (x * 275)) * (((x * x) * x) * y)) + ((418 * y) + (x + 2267))", "(((y * y) * ((y + ((y * ((x * (x * x)) + x)) + x)) * (((y + (x + x)) + 481) * y))) * 674) + x", "607 + ((((x + (407 * x)) + x) + ((165 * ((((y + 156) + x) + 626) * x)) * ((386 * (y + x)) * 52))) + 397)", "(((837 * ((700 * x) + (307968 * ((x * x) * (x + 1159))))) + (541 + (845325 * ((173 + x) + x)))) + x) * (844367 + y)", "(((((x + 266) + y) + 1017) * 565) + 511) + ((y + ((577 * (y + 190)) + (y + ((y + x) * (y * 307895))))) * 835)", "(((237 * x) * x) * 943) + ((((((x + (y + y)) + (1200930962 * (y * (y + x)))) * y) * (x * y)) * y) * 195)", "(((((y + y) + y) * (x + y)) * ((((y * (x * x)) * 740) * y) + 191648)) + (y * x)) * (924 * (x + 748))", "y * ((((x + (173 + y)) + (871 + ((x + y) + (85 * (y * (x + y)))))) * x) + (x + ((x + x) + x)))", "51 * (y * (((((811 + (619 + x)) * ((y + x) + ((x + x) * (198 + (y * y))))) + 440) * 1044) * (x * x)))", "((((1830 * ((172 + ((((y + y) * (y * 930)) + ((x * x) * 582)) * (x + 963))) + 644286)) * y) + 462) * x) * 348", "((464 * (((((110864650 * y) * 278) + (y * x)) + ((521 + (y + (x * 216))) + ((x * 424) + y))) * x)) * y) * 45", "(((y * ((x + (((2167 * y) + 229) * 393)) + (x * (x * ((x + 554) * x))))) * 106) + ((x + 333326) * y)) * 19", "x + (y + ((622 * y) + ((((65 + ((934 * y) + y)) + (713 + (y + y))) * (785 + (184 + (y + y)))) + y)))", "((970 + y) * 479) + ((215 + x) * ((((8 + (x + (862 * (y * (((y * x) + 32760) + 1229))))) + 582) + x) + y))", "((((((((x + 718) * y) * 708) + 596) * 702) * (y + (y * (437 * x)))) + ((76 + (53 + x)) + 921)) * y) + 341", "(988 * (743 + (((y * ((y * y) + (((((781 * (x + 930)) + (843 * y)) * x) * 386) + 184932))) + 752) + y))) * 807", "x + (((684 + ((790 * (541 + ((272288 * ((y + x) + (779 * y))) * x))) * y)) + ((207928 + (325 + y)) + 53)) * 25)", "x + (((x * x) + ((((491 * (y + y)) * 712) + (250488 * (22790196 * (y + 675)))) + (x * x))) + ((68 + y) + 389))", },
	{"(53 * (((((144 * ((((((y * 381) + 1013) * y) * 736) * y) * ((y * (x + 23)) + x))) * 167) * y) * 365) * 404)) + 10709", "(101462 + (y + 445950120)) + ((913 + ((((446 * ((x + 1) + (850 + (217 * (((y * 781) * y) * x))))) * y) + y) * 748)) + 408)", "(974 * y) * ((((y + 647) + (((y + ((y * 883) * 366)) * ((x * (((y + 278) + y) + y)) * 753)) * 722)) * x) + y)", "((((958 * y) * 922) + ((x + ((y + 503) + (312 * ((65 + y) * y)))) * 625)) * ((803 * (y * y)) + 227)) + (y + y)", "(400 + (x * ((991 + (261 + y)) * y))) + (623 + ((x + (x * ((((y * (y * 360050)) * 656) + (284 * y)) * 754))) + x))", "(((x + y) * ((y * 216) * ((x + x) * 815))) * ((402 * x) * (((140 + y) + x) + ((90 * (79587 * y)) + x)))) + y", "(454 + ((114 + ((36206 + ((160270 + y) + ((427 * ((675 * x) * (y + x))) + ((679 * x) * x)))) + (316 + y))) * 873)) + y", "((((y + ((y + (y * 514)) + ((445 * (((166 * x) + x) + y)) * y))) * 181) * ((46 * (200 * x)) * x)) * y) + y", "485 * (((268 * x) * (29151 + (4 * (y * (y * ((((x + x) * 36) + (((y + 307) * (363 + x)) + 38)) + 852)))))) + x)", "(x * (151522 * ((168 + (y * (y * y))) + (y + x)))) + (((((x + ((944 * (y + x)) * x)) + 494) + y) + 768) * y)", "(((y + ((((y * (x * ((1837 * x) * (y + 152)))) * y) * 746) + (580 * (((896 * x) + x) * 819)))) * y) + y) + x", "((x + ((x + (((((527 + x) + x) * ((x + x) + 1196)) * x) * 329)) + x)) + ((150 + y) * y)) + ((577 + x) + 196)", "(((((402 + y) * ((566 * y) + x)) + 1010) * (846 * ((814 + ((x * x) * ((y * 674) * y))) + (x * y)))) * y) + y", "(((y + x) * (y + ((((((188 + x) * y) + x) * 771) * 872) + x))) + (((x + (y * 249)) + x) + x)) * (y * x)", "((y + x) * y) + ((((424575 + (((((483 + (y * 582)) + x) + 479) + 633) * y)) + 2013) + ((x * x) * (x * 720))) * 786)", "(y + (x + (690 + ((x + ((((x + 635) + x) + (x * 707)) + y)) * 862)))) * (((x + 933) + (y * (y + y))) + 545)", "(y * (((923 * ((((1556 + (y + (1239 + y))) + 617) + x) + (541 + (((492 * (x * y)) * 491) + 613)))) + 323) * x)) * y", "(x * (710 * (803 + (((((y * 142) * (((x * 417) + (y * 521)) * 356)) * 917) + (y * 29280)) + (711990 * y))))) + (y + x)", "((538 + (((x * x) * y) * (353 + (591274 * x)))) + (((432 * x) * x) * 269)) * ((x * (936 + (585 + x))) + (y + y))", "(((77 + (((((y + y) + 1007) + 506) * (((y * 329) * 47520) + (464 + x))) * 803)) * ((y + 357) + x)) * x) + (y * x)", },
	{"(x + (((x + (y * 292)) * (397 + y)) + (24955 * (((452 + (((925 * x) + x) * (x + x))) * ((58 * x) + 376)) + 962)))) + y", "y * (((((y + y) + (538 * ((y + 18) * 845))) * y) + ((x * ((y * 856) * 400)) + ((x + x) + (288 + x)))) * (y + y))", "(345 + ((((((x * (x + 844)) + (x + (y + y))) + x) + ((927 * x) + ((229 * x) * y))) + (y + 399)) * 377)) + (y * y)", "((33817013 + (x + ((y * (y * 790)) + (((x + 112) + ((x + 645) * ((x + 806) + 788))) * y)))) * ((y + (y + 1007)) * y)) + y", "((((y + (y + (x + y))) * (y * ((y + ((794 + x) + 209)) * (31590 + x)))) * y) * ((y * 305) + (240093 * x))) * (y + 137)", "(((15 * (((y * (y * y)) * (x + 316)) + (y * (343 + (y + (y * ((196 + y) + (119 + y)))))))) * (83 + y)) + 984) + 317", "(((y * ((588 + (872 * x)) + ((x + ((x + 96) * (x * 600))) + x))) + (693 + (((y + y) * 872) + x))) * 558) + (371 + y)", "(227 * ((616476 + (((y * (900 * (x * x))) + 276) * (838 * y))) + (x + (876 + ((817 + (x + x)) + ((378 * y) + 561)))))) * 287", "(y * (x * (((y * ((y + 767) * x)) + x) + (1660 * (((x * 293) + y) + (x + (y * x))))))) + (((846 * x) + y) * x)", "((y + ((y * 205) * (1666 + x))) + (((869 + (((((x * 41) * 940) + 905) * 236) * x)) + y) * y)) * (((x + 524) * 459) * y)", "((((274 * (x + (306 * y))) * x) + (596732844 * (y * (((891 + (779 * y)) + 946) + (683 + y))))) + y) + (x * (1442 * (y * 777)))", "27623728 + ((x * ((740 * (y * x)) + x)) + (((y * (((115 * (x + (759 * (x * 659)))) + 137) + 190444)) + (807877 + y)) * (x * x)))", "(((x * (532 * y)) + (172 + ((((x * (845 + y)) * y) * x) * (x + 742)))) * (x * (((y * 260) + 299) + 817))) * (330 * y)", "(((99 + (y * 156)) * (x + y)) * (469 + (390 + ((x * (149 * (117 * (y + ((711 * x) * (10 + y)))))) + (y * x))))) * 192", "(y + ((y + ((y + (((y + (x + (y * (x + 125)))) + (31 + (x + (x * 176)))) + ((821 + y) * x))) * x)) + 893)) + 1416", "((987 * y) + x) + (((y + 370) * ((904 * x) + (x + (193 + ((x * ((1019 * y) + ((513 + y) * (y + x)))) * y))))) * y)", "((((x * y) + 299) * (348 * ((y + 769) * (406 + (387 + x))))) + (x * (y * (527 + ((y + y) * x))))) + ((y * x) + x)", "((((x + ((y + ((x * 364) + 304)) + (x * 677))) * ((x * (782 * (377 + y))) + y)) * 410) + 967) * (x * (x * (331 + y)))", "(19 * x) + ((478 * ((x + (399 * y)) * ((132 * x) * ((y + 436) * y)))) * ((y + (x + 787)) * (((636 * y) + x) + 443435)))", "((107 * y) + x) * ((((((((y * 315) + 585) * x) * (70 + y)) + 155477) + y) + (((((x * 126) * x) + 961) * 140) * y)) + 503)", },
	{"(((((747 + (85 + y)) * (7392 * (x * (((y + (((4 * y) * (x * x)) + 365754)) * 392) * 585)))) * y) * (y * 376640)) * (y + 9660)) * 545", "(146 + (((799 * (x + ((563 + ((((688 + y) * 341) + (y + (y * 171836))) * ((((y + 55) + x) * y) * 707))) + 933))) + x) * 245)) + 357", "((442 * ((((385 * (x * x)) * 444) + 459) + ((387 * ((853 + ((906 * ((69 + (y * x)) * 728)) * 988)) + (y * 944))) + 147347109))) + 169) * 673", "((907 + ((816219 * x) * x)) + 278) + ((x * y) * ((((618 * x) * ((420 * ((x + 641) * x)) * ((x * x) * x))) + y) + (x * y)))", "(109 + (((((315 * y) * y) + y) + (((717948 + (y + 499)) * x) + x)) * y)) * ((x + ((y * (y + y)) + y)) + (128 + (564 * x)))", "(600216 + (((x * (808 + (x + (890 * ((898 * (y * ((984 * ((x * x) * 722)) * 254))) + ((1003 + y) + y)))))) * (y * 590)) + y)) * x", "((610 * ((((((((x * y) + y) * (x * 559)) * (y * 1492)) + (x * 966)) + 865) + (y + (480 * x))) + 502)) * (x + (938 * x))) + x", "529 + ((3400 + ((x + 718) + ((187 + ((x * 108) + (931 + x))) + (233461 + (y + y))))) * (((130 * (y + 76)) + ((x * x) * 651)) + x))", "((((((x * 940) + 331) * x) * ((862 * ((x * 701) + (y + 61))) * y)) + y) + (((x + (237 + ((x + y) + x))) * x) * 291)) + 697921", "y * (((482 + ((242 * (((53 * (36494 + x)) + (253184 * x)) + ((904 + x) * (y * 569)))) + ((88 + (y * y)) + y))) * (x + y)) * y)", "(((y + (((911 + (893 + (621 * (x * x)))) * y) + (939 * (y * (268 + (((x * 251) * (y * 1678)) * 290)))))) + (x * y)) * 499) + x", "((((((y * (363 * ((((y * x) * 473) * (369 * x)) * y))) + x) + (y + (((x * 562) + 324) + 944))) + (x * 122)) * x) * x) * x", "((((475 + x) * y) + ((x + 629) + (x * x))) + y) * (524 + (x + ((578 * (y * (x + ((y * ((y * x) * 476)) * x)))) + x)))", "(y * y) * (((x * x) * (x + x)) + (1132 * ((224 + ((x * y) * ((x + y) + 614))) + (796 + (x * (519 * ((y + 352) * y)))))))", "35 * (277 * ((y + (((x + (407 * ((y + 555) + x))) + (396104 + (230 * (((y + (x * 783)) * ((y * x) + y)) + x)))) + 452)) * y))", "((((y + (940 + (y + 412))) * x) * ((((767 * (x * 489)) + (50 + (y * (342376 + ((x * y) * 660))))) * x) * 29)) * 128) * (y + 729)", "((((518 + (((y + 926) + y) + y)) * y) + x) * (169 * (((x * (342 + (((y * x) + ((y * 821) * x)) + 735))) * y) + 578))) * 8", "((((442 + (((((413440 * y) * (432 * x)) * (y + (902 + x))) * x) * (x * (((y + (x * x)) * x) * y)))) * 444) * y) * 962) + 862", "((590 * (515 + ((y + ((x * 793) * (x * 822))) * 284820))) + (y + (819 + (((x + (x + y)) * 698) + ((x * 555) + (1005 + y)))))) * y", "(y + 851) * ((y * 523) * ((((x * 526) * 834) + 216) * (860 + (y * (472 * ((((((x * 926) + (y * 592)) + 883) + y) + y) * x))))))", },
	{"x + ((x * (607 * ((((x + (((x * 306) * (628 + x)) * (((y * ((1014 * (y * y)) * (59 + x))) * y) * y))) * x) + x) + y))) * 441)", "(((((x * 1040) * 746) * (x + ((x * y) + y))) * (((x * 16815) * (x + y)) + ((((550 * (x * (x + 474))) + 303) * x) + 1143))) + 579) + x", "y * ((x * y) * ((636 * (((x + (401 * (((905 * (((583 + y) + y) * 812)) * y) + (x + 467)))) * x) + y)) * (((521 + y) * x) * x)))", "(((490 * x) * 118) + ((x + ((x * (((x * 440) + 385) * x)) + 136)) + 458)) * (((x + 706) + ((684744 + (((y * 903) + 345) + 1367)) * 565)) + x)", "1682964480 * (189 + (((793 * ((y * 806) * 907)) * 391) * ((x * ((y * 770) + (627 + ((((y * x) * (719 * (1004 * y))) + 96) * (x + 33))))) * 858)))", "(((560 * (766 + x)) * (((y + (344 + ((886 * x) + ((y * x) * 402)))) + (((339 + (x * ((776 * y) * x))) * x) * 43)) + x)) * x) + y", "(x * ((x * ((x + (((((x + 225) * 562) * (y + (473 * ((x * y) + (402 + y))))) * x) + (x + 294))) + (394 * x))) + 830)) * (711 * y)", "(y * ((((x * y) * 583) + 381) * (((((2572 * y) * 1654) + 867) * (200802 + ((1084 * (((((495 + x) + y) * y) * y) * x)) + 468))) + x))) + x", "y + ((y + (((((214 + x) + 713) * y) * 292) * x)) + ((((((1266 + ((x + ((x + x) + y)) + 132)) + x) + (859 * x)) * y) * x) + y))", "x * (((553 + x) * 748) + (((981 + ((((675 * x) * y) * x) + 292)) + (((((682 * (1017 + x)) + 962) * 601) + y) * (717 * x))) + (y * x)))", "303 * ((572 + (977 + (y * 201))) + ((((x * 733) + (y * (((87429912 * ((y + ((y * 1003) * 1255)) + x)) + 1387286) + y))) + 429) + ((x * 947) + 954)))", "((((407 + (287 * ((577 + (124 + (x * (x * (606 * (918525 + x)))))) + 413))) + ((256 + (x * x)) * ((828 + (x + y)) + x))) * y) + 998) * 960", "(((y * (x + ((y * ((666 * x) * x)) * y))) * 225) + ((((((y * (x * y)) * 378) + y) * y) * (593 * y)) * (x + (x * 250)))) * 315", "((y + (((((((990 * (x * (x * (x * (((x * 60) + y) + x))))) * x) * (476 * x)) + y) * 525) + y) * x)) * ((747 * y) + x)) * 787", "(((615 + (((((298 + y) * 707) * x) * 252) + ((557 + x) * 695))) * x) + ((123540 + x) + 811)) + (y + (((((x + 57) + 677) * x) + 237) * y))", "((x * (429 + ((657 * y) + ((y * x) * x)))) * ((y + (y + (x * 987))) + (((106 + (y * x)) + 391) + ((962 * y) + (826 * y))))) * 539", "(449 + ((y + (y * (((y * (655 * ((y * x) * ((y + (x + y)) * x)))) + 17578) + (x * (y * y))))) + (582 + x))) * (y + (x + 266))", "(357908760 * (x * ((859 + ((442 + y) + (x * (x + (y * (786 * ((y + ((100 * (920 + (186 * x))) + ((y * 345) + x))) + 720))))))) + x))) * 542", "(1017 + (((((668450 + (x * 784)) + (x + (553 * ((929 * y) + 92)))) * (x + y)) + (138030 + y)) + ((18 + x) + 651))) + (((x + 467) * x) * x)", "((((y + x) * (x * ((((244280 + y) * 1624) + ((984 * (x * 309)) + ((x + y) * y))) * (((y + 236964) + (x + 334)) + x)))) * x) * y) + 989", },
};
//END

MemoryAccessComplexityKernelProvider::MemoryAccessComplexityKernelProvider() :
KernelProvider("", "Number of Basic Operations") {};

string MemoryAccessComplexityKernelProvider::getKernelCodeBegin() {
    return kernelString;
}

string MemoryAccessComplexityKernelProvider::getKernelCodeEnd() {
    return kernelEnd;
}

float MemoryAccessComplexityKernelProvider::runKernel(ocl_device *device, int whichAccess) {
    ocl_kernel kernel(device, getKernelString(whichAccess));
    
    
    unique_ptr<float[]> memory(new float[benchmarkMemSize]);
    for (int i = 0; i < benchmarkMemSize; i++) {
        memory[i] = i * 42.f;
    }
    
    ocl_mem deviceMemory = device->malloc(benchmarkMemSize*sizeof(float));
    deviceMemory.copyFrom(memory.get());
    kernel.setArgs(deviceMemory.mem());
    int wgSize = getWorkgroupSize(device, benchmarkMemSize);
    // Execute using N-sized work-groups with a total of N work-items
    int i = kernel.timedRun(wgSize, benchmarkMemSize);
    
    // Wait until the kernel is done executing
    device->finish();
    float runtime = kernel.getRunTime(i);
    return runtime;
}

std::string MemoryAccessComplexityKernelProvider::getKernelString(int whichAccess) {
    stringstream buf;
    whichAccess--;
    string addressingCode = memoryAccesses[whichAccess/20][whichAccess%20];
    
    buf << this->getKernelCodeBegin();
    if (addressingCode.find('x') != string::npos) {
        buf << xDef;
    }
    if (addressingCode.find('y') != string::npos) {
        buf << yDef;
    }
    buf << kernelContinuation;
    buf << addressingCode;
    buf << this->getKernelCodeEnd();
    return buf.str();
}