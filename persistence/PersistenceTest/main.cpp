/* 
 * File:   main.cpp
 * Author: aherz
 *
 * Created on March 31, 2011, 4:28 PM
 */

#include <stdio.h>
#include <cstdlib>
#include <time.h>

#include <math.h>
#include <limits>
//#include <xmmintrin.h>
//#include <immintrin.h>


#include <vector>

#include "bitcount.hpp"
#include "wRefCounter.hpp"

timespec diff(timespec& start, timespec& end)
{
	timespec temp;
	if ((end.tv_nsec-start.tv_nsec)<0) {
		temp.tv_sec = end.tv_sec-start.tv_sec-1;
		temp.tv_nsec = 1000000000+end.tv_nsec-start.tv_nsec;
	} else {
		temp.tv_sec = end.tv_sec-start.tv_sec;
		temp.tv_nsec = end.tv_nsec-start.tv_nsec;
	}
	return temp;
}

double diff_sec(timespec& start, timespec& end)
{
    timespec t=diff(start,end);
    return ((double)t.tv_sec)+(t.tv_nsec/1000000000.0);
}

void get_time(timespec& t)
{
    clock_gettime(CLOCK_PROCESS_CPUTIME_ID, &t);
}


using namespace std;

class PersistenceNode;
class PersistenceRoot;
class cVersion;

class IPersistentDS
{
public:
    virtual void Dump(cVersion* version,uint32 root)=0;
    virtual IPersistentDS* ShallowCopy()=0;
    virtual uint8 GetChildCount()=0;
    virtual PersistenceNode* GetChild(uint8 child)=0;
    virtual void SetChild(uint8 child,PersistenceNode* node)=0;
};


//typedef

struct VersionInfo
{
    uint64 bits; //actual bits
    uint32 skip; //number of skipped batches (before this)
    uint32 pos; //start pos
    uint32 count;//# of bits set in this batch


    VersionInfo(uint32 bpos,uint32 skp,uint32 ps,uint32 prev_count)
    {
        bits=1<<bpos;
        skip=skp;
        pos=ps;
        count=prev_count+1;
    }

    VersionInfo() {}
};

template <int I=0>
struct BitMask
{
    uint64 BitMask1[64] __aligned__;
    uint64 BitMask2[64] __aligned__;
    
    void initBitMask()
    {
        BitMask1[0]=0;
        for(int i=1;i<64;i++)
        {
            BitMask1[i]=BitMask1[i-1]|1<<(i-1);
        }

        //invert here so we don't need to do it inside GetVersion
        BitMask2[63-0]=0;
        for(int i=1;i<64;i++)
        {
            BitMask2[63-i]=BitMask2[63-(i-1)]|1<<(i-1);
        }

    }
    
    BitMask()
    {
        initBitMask();
    }
};


static BitMask<> bitmask;

class cVersion
{    
public:
    DynArray<VersionInfo*> mVersion;
    
    cVersion(uint32 pos)
    {
        mVersion.push_back(new VersionInfo(pos%64,pos/64,pos-pos%64,0));
    }

    cVersion()
    {
    }

    uint32 getVersionID()
    {
        uint32 res=mVersion.at(mVersion.size()-1)->pos;
        res+=64-__builtin_clzl(mVersion.at(mVersion.size()-1)->bits);
        return res;
    }

    void set(int pos)
    {

        uint32 opos;
        uint32 i;
        
        if(pos<64)
        {
            mVersion.at(0)->bits|=1<<pos;
            mVersion.at(0)->count++;//FIXME: check if bit was set??
            return;
        }
        else
        {
            opos=pos;
            i=binaryFindPos(pos,0,mVersion.size()-1);
            pos-=mVersion.at(i)->pos;
        }

        if(pos<64)
        {
            //we found the data
            mVersion.at(i)->bits|=1<<pos;
            mVersion.at(i)->count++;//FIXME: check if bit was set??
            //mVersion.at(i).most=max(pos,mVersion.at(i).most); //do we need the most significant bit??
        }
        else
        {
            //data is missing
            if(i<mVersion.size()-1)
            {
                mVersion.insert_at(i+1,new VersionInfo(pos%64,pos/64,opos-opos%64,mVersion.at(i)->count));
                mVersion.at(i+2)->skip-=pos/64;
            }
            else
            {
                mVersion.push_back(new VersionInfo(pos%64,(pos-1)/64,opos-opos%64,mVersion.at(mVersion.size()-1)->count));
            }
        }
    }

    void clear(uint32 pos)
    {
        
    }

    bool equal(cVersion* pVersion)
    {
        if(pVersion->mVersion.size()!=mVersion.size())
            return false;

        for(uint32 i=0;i<mVersion.size();i++)
        {
            if(pVersion->mVersion.at(i)->bits!=mVersion.at(i)->bits)
                return false;
        }

        return true;
    }

    cVersion* copy()
    {
        cVersion* res=new cVersion();
        res->mVersion.prealloc(mVersion.size());
        
        for(uint32 i=0;i<mVersion.size()-1;i++)
        {
            res->mVersion.push_back(mVersion.at(i));//sharing??
        }
        
        VersionInfo* nvi=new VersionInfo();
        *nvi=*mVersion.at(mVersion.size()-1);
        res->mVersion.push_back(nvi);

        return res;
    }

    inline uint32 binaryFindPos(uint32 pos,uint32 min ,uint32 max)
    {
        //shortcuts for ver0 and last ver
        if(pos<64)
            return 0;

        if(mVersion.at(max)->pos==pos)
            return max;
        uint32 bpos=binaryFindPosRec(pos,min,max);
//        uint32 lpos=linearFindPos(pos);

        return bpos;
    }

    int linearFindPos(uint32 pos)
    {
        uint32 i=0;
        
        while(i<mVersion.size()-1&&mVersion.at(i)->pos<pos+64)
            i++;

        return i;
    }

    inline uint32 binaryFindPosRec(uint32 pos,uint32 pmin ,uint32 pmax)
    {
        uint32 mpos;
        uint32 half;
        
        while(pmin<pmax)
        {
            half=pmin+((pmax-pmin)>>1);
            mpos=mVersion.at(half)->pos;
            if(mpos!=pos)
            {
                if(mpos<pos)
                    pmin=half+1;
                else
                    pmax=half-1;
            }
            else
                return half;
        }
        
        return pmin;
    }


    inline uint32 versionToOffset(cVersion* pVersion)//self is availability
    {
        uint32 ia=pVersion->mVersion.size()-1;
        uint32 ib=mVersion.size()-1;

        if((ia|ib)==0) //fast path for few versions
        {
            uint64 vand=(mVersion.at(0)->bits)&(pVersion->mVersion.at(0)->bits);
            uint32 firstbit=__builtin_clzl(vand);
            uint64 mask=bitmask.BitMask2[firstbit];
            uint64 bitoffset=mask&mVersion.at(ib)->bits;
            uint32 offset=__builtin_popcountl(bitoffset);

            return offset;
        }

        while(ia>=0&&ib>=0)
        {
            if(mVersion.at(ib)->pos>pVersion->mVersion.at(ia)->pos)
                ib=min(ib-1,binaryFindPos(pVersion->mVersion.at(ia)->pos,0,ib));
            else if(mVersion.at(ib)->pos<pVersion->mVersion.at(ia)->pos)
                ia=min(ia-1,pVersion->binaryFindPos(mVersion.at(ib)->pos,0,ia));
            
            if(mVersion.at(ib)->pos==pVersion->mVersion.at(ia)->pos)
            {
                uint64 vand=(mVersion.at(ib)->bits)&(pVersion->mVersion.at(ia)->bits);

                if(vand!=0)
                {
                    uint32 firstbit=__builtin_clzl(vand);
                    uint64 mask=bitmask.BitMask2[firstbit];
                    uint64 bitoffset=mask&mVersion.at(ib)->bits;
                    uint32 offset=__builtin_popcountl(bitoffset);
                    
                    //for(int i=ib-1;i>=0;i--)
                    if(ib>0)
                        offset+=mVersion.at(ib-1)->count;

                    return offset;
                }
                else
                {
                    ia--;
                    ib--;
                }
            }
        }
    }
};

class PersistenceRoot
{
    uint32 mCurVersion;
    uint32 mCopyTag;
public:
    PersistenceRoot(uint32 ver=0)
    {
        mCurVersion=ver;
    }

    cVersion* getCurVersionBit()
    {
        return new cVersion(mCurVersion);
    }

    cVersion* getNewVersionBit()
    {
        return new cVersion(getNewVersion());
    }

    inline uint32 getNewCopyTag() //thread safe copy tag generator
    {
        return ++mCopyTag;//__sync_add_and_fetch(&mCopyTag,1);
    }

    uint32 getCurVersion()
    {
        return mCurVersion;
    }
    
    uint32 getNewVersion()
    {
        mCurVersion++;//FIXME thread safety
        //uint32 res=__sync_add_and_fetch(&mCurVersion,1);
        //ASSERT(mCurVersion);
        
        if(!mCurVersion)
            throw "more than 2^32 versions of a DS are not supported";
        
        return mCurVersion;
    }
};


//#define SSE

class PersistenceNode
{
    //vector<IPersistentDS*> mVersions;
    DynArray<void*> mVersions;
//    uint64 mAvilableVersions;
    cVersion *mAvilableVersions;
    PersistenceRoot* pRoot;
protected:
//    cVersion* mCopyTag;
    uint64 mCopyTag;
public:


    PersistenceRoot* getRoot()
    {
        return pRoot;
    }

    uint32 getVersionFieldCount()
    {
        return mAvilableVersions->mVersion.size();
    }

    PersistenceNode(IPersistentDS* pDS,cVersion* initialVersion,PersistenceRoot* root)
    {
        mVersions.push_back(pDS);
        mAvilableVersions=initialVersion->copy();
        pRoot=root;
        mCopyTag=0;
    }

    cVersion* AddVersion(IPersistentDS* pDS,cVersion* to)
    {
        mVersions.push_back(pDS);
        uint32 nverpos=pRoot->getNewVersion();
        mAvilableVersions->set(nverpos);
        cVersion* pnv=to->copy();
        pnv->set(nverpos);
        return pnv;
    }

    template<class T>
    T* GetVersion(cVersion* version)
    {
        if(!mVersions.isSingle())
        {
            uint32 offset=mAvilableVersions->versionToOffset(version);
            return (T*)(mVersions.atMultiple(offset));
        }
        else
            return (T*)mVersions.atSingle();
    }
    
    void Dump(cVersion* version,uint32 root)
    {
        GetVersion<IPersistentDS>(version)->Dump(version,root);
    }
    
    PersistenceNode* copy(PersistenceRoot* pnewroot,cVersion* version,uint32 copytag)
    {
        //mCopyTag=version;
        mCopyTag=copytag;//unique tag from our root
        IPersistentDS*pDS=GetVersion<IPersistentDS>(version);
        IPersistentDS*pNDS=pDS->ShallowCopy();
        PersistenceNode* pn=new PersistenceNode(pNDS,pnewroot->getCurVersionBit(),pnewroot);
        
        const uint8 max=pDS->GetChildCount();

        for(uint8 i=0;i<max;i++)
        {
            if(pDS->GetChild(i)!=NULL&&
                    (pDS->GetChild(i)->mCopyTag!=copytag))
            {
                pNDS->SetChild(i,(pDS->GetChild(i)->copy(pnewroot,version,copytag)));
            }
        }

        return pn;
    }
    
    PersistenceNode* copyupdate(PersistenceRoot* pnewroot,cVersion* version,uint32 copytag,PersistenceNode* n,IPersistentDS *data)
    {
        //mCopyTag=version;
        mCopyTag=copytag;//unique tag from our root
        IPersistentDS*pDS=GetVersion<IPersistentDS>(version);
        
        IPersistentDS*pNDS;
        if(this==n)
            pNDS=data->ShallowCopy();
        else
            pNDS=pDS->ShallowCopy();
        
        PersistenceNode* pn=new PersistenceNode(pNDS,pnewroot->getCurVersionBit(),pnewroot);
        
        const uint8 max=pDS->GetChildCount();

        for(uint8 i=0;i<max;i++)
        {
            if(pDS->GetChild(i)!=NULL&&
                    (pDS->GetChild(i)->mCopyTag!=copytag))
            {
                pNDS->SetChild(i,(pDS->GetChild(i)->copyupdate(pnewroot,version,copytag,n,data)));
            }
        }

        return pn;
    }    

};



template<class T>
class cVersionPointer
{
    PersistenceNode* pNode;
    cVersion *mVersion;
    PersistenceRoot mRoot;
    
public:
    cVersionPointer(PersistenceNode* pn,cVersion* mv):pNode(pn),mVersion(mv)
    {}
    
    PersistenceNode* New(T* data)
    {
        new PersistenceNode(data,mVersion,&mRoot);
    }
    
    T* Get(PersistenceNode* pn)
    {
        return pn->GetVersion<T>(mVersion);
    }
        
    T* Get()
    {
        return pNode->GetVersion<T>(mVersion);
    }
    
#define MAX_VERSION_COUNT 63
    
    cVersionPointer* AddVersion(PersistenceNode* node,T* data)
    {
        if(node->getRoot()->getCurVersion()<MAX_VERSION_COUNT)//can add version?
        {
            cVersion* vnew=node->AddVersion(data,mVersion);
            return new cVersionPointer<T>(pNode,vnew);
        }
        else //deep copy
        {
            return new cVersionPointer(node,pNode,data);
        }
    }
    
    cVersionPointer(PersistenceNode* node,PersistenceNode* rnode,T* data)
    {
        mVersion=mRoot.getCurVersionBit();
        pNode=rnode->copyupdate(&mRoot,mVersion,rnode->getRoot()->getNewCopyTag(),node,data);
    }
    
    cVersionPointer(T* data)
    {
        mVersion=mRoot.getCurVersionBit();

        pNode=new PersistenceNode(data,mVersion,&mRoot);        
    }
    
    void Dump(uint32 root)
    {
        pNode->Dump(mVersion,(uint32)&mRoot);
    }
    
};


FILE* out; //ugly hack

struct BinTree:public IPersistentDS
{
public:
    int val;
    PersistenceNode* left,*right;

    BinTree()
    {}

    BinTree(int v,PersistenceNode* l=NULL,PersistenceNode* r=NULL)
    {
        val=v;
        left=l;
        right=r;
    }

    BinTree(BinTree* bt)
    {
        val=bt->val;
        left=bt->left;
        right=bt->right;
    }

    void Dump(cVersion* version,uint32 root)
    {
        //printf("(%d): val = %d;",version,val);
        uint32 vid=version->getVersionID();

        if(left)
        {
            fprintf(out,"\"(%d,%d): val = %d\"->\"(%d,%d): val = %d\"",root,vid,val,root,vid,left->GetVersion<BinTree>(version)->val);
            left->Dump(version,root);
        }
        
        if(right)
        {
            fprintf(out,"\"(%d,%d): val = %d\"->\"(%d,%d): val = %d\"",root,vid,val,root,vid,right->GetVersion<BinTree>(version)->val);
            right->Dump(version,root);
        }
    }

    
    //compiler generated functions:
    IPersistentDS* ShallowCopy()
    {
        return new BinTree(this);
    }

    uint8 GetChildCount()
    {
        return 2;
    }

    PersistenceNode* GetChild(uint8 child)
    {
        switch(child)
        {
            case 0:return left;
            case 1:return right;
            default:return NULL;
        }
    }

    void SetChild(uint8 child,PersistenceNode* node)
    {
        switch(child)
        {
            case 0:
            {
                left=node;
                break;
            }
            case 1:
            {
                right=node;
                break;
            }
            default:
                break;
        }
    }


};

class NPBinTree
{
public:
    int val;
    NPBinTree* left,*right;
    
    NPBinTree(int v,NPBinTree* l=NULL,NPBinTree* r=NULL)
    {
        val=v;
        left=l;
        right=r;
    }
};


#define align __attribute__((aligned(16)))

//#define ASM(X) __asm__ volatile (x++"\n")
//const uint32 cst[] align ={1,2,3,4};
const uint32 mask[] align ={1<<31,1<<31,1<<31,1<<31};
const uint32 one[] align ={0x3f800000,0x3f800000,0x3f800000,0x3f800000};
//const uint32 one[] align ={1,1,1,1};
uint32 scratch[] align ={0,0,0,0};
//const uint32 indexlu[] __aligned__ ={0,0,0,0};

bool init=false;

uint32 fivearySearchSSE(uint32* list,uint32 length,uint64 value)
{
//    __m128i _mm_loadu_si128();
    uint64 valval=value|value<<32;

    //setup data
    __asm__ volatile ("movq %0, %%xmm4" : : "r" (valval));
    __asm__ volatile ("movq %%xmm4, %%xmm0" : :);
    __asm__ volatile ("unpcklps %%xmm0, %%xmm4" : :);
    
    //__asm__ volatile ("movdqa (%0), %%xmm5" : : "r" (cst));
    if(!init)
    {
    __asm__ volatile ("movdqa (%0), %%xmm6" : : "r" (mask));
    __asm__ volatile ("movdqa (%0), %%xmm7" : : "r" (one)); //zero
    init=true;
    }

    uint32 min=0;
    uint32 max=length;

    uint32 index;

    while(min+3<max)
    {
        
        uint32 width=(max-min)/5;
        
        scratch[0]=list[min+width];
        scratch[1]=list[min+width*2];
        scratch[2]=list[min+width*3];
        scratch[3]=list[min+width*4];
                
        __asm__ volatile ("movdqa (%0), %%xmm3" : : "r" (scratch)); //move tests into reg
        __asm__ volatile ("psubd %%xmm4,%%xmm3" : : ); //compare tests with vals
        __asm__ volatile ("pand %%xmm6,%%xmm3" : : ); //mask off sign bit
        __asm__ volatile ("psrld $31,%%xmm3" : : ); //right shift so it's se to 0x1
        __asm__ volatile ("dpps $0xf1,%%xmm7,%%xmm3" : : ); //abuse dpps to accum
        __asm__ volatile ("movd %%xmm3, %0" : "=r" (index) : ); //compare tests with vals

        if(scratch[index]==value)
            return min+index*width; //found

        min=min+(index)*width;
        max=min+width-1; //old_min+(index+1)*width-1
    }

    if(min>=max)
        return max;

//    float findex;
    
    __asm__ volatile ("movdqu (%0), %%xmm3" : : "r" (&list[min]));
    __asm__ volatile ("psubd %%xmm4, %%xmm3" : : );
    __asm__ volatile ("pand %%xmm6,%%xmm3" : : ); //compare tests with vals
    __asm__ volatile ("psrld $31,%%xmm3" : : ); //compare tests with vals
    __asm__ volatile ("dpps $0xf1,%%xmm7,%%xmm3" : : ); //abuse dpps
    __asm__ volatile ("movd %%xmm3, %0" : "=r" (index) : ); //compare tests with vals
    
    return min+index;

}


uint32 binaryFind(uint32* list,uint32 length,uint32 value)
{
    uint32 mpos;
    uint32 half;

    uint32 pmin=0;
    uint32 pmax=length-1;

    while(pmin<pmax)
    {
        half=pmin+((pmax-pmin)>>1);
        mpos=list[half];
        if(mpos!=value)
        {
            if(mpos<value)
                pmin=half+1;
            else
                pmax=half-1;
        }
        else
            return half;
    }

    return pmin;
}

//#define LOOP 1  //6 secs at release
#define LOOP 20000000
#define VERSIONS 300
//#define LOOP 200000000
//#define VERSIONS 200000

void wrefc()
{
    float* vec=(float*)malloc(sizeof(float)*4);
    
    wRefCount* mrc1=wRefCount::createObject(vec);

    ((float*)mrc1->GetData())[0]=1.f;

    wRefCount* mrc2=mrc1->AddRef();

    mrc1->Release();

    float res=((float*)mrc2->GetData())[0];

    mrc2->Release();

    int i=0;
}

#define SIZE 100000000

void oldversions()
{
    out=fopen("out.dot","wb");
    
    PersistenceRoot root;
    
    cVersion* v0=root.getCurVersionBit();

    PersistenceNode *pRoot=new PersistenceNode(new BinTree(5),v0,&root);

    pRoot->GetVersion<BinTree>(v0)->left=new PersistenceNode(new BinTree(1),v0,&root);
    pRoot->GetVersion<BinTree>(v0)->right=new PersistenceNode(new BinTree(6),v0,&root);

    cVersion* v1=pRoot->GetVersion<BinTree>(v0)->left->AddVersion(new BinTree(2),v0);
    pRoot->GetVersion<BinTree>(v1)->left->GetVersion<BinTree>(v1)->left=new PersistenceNode(new BinTree(8),root.getCurVersionBit(),&root);

    cVersion* v2=pRoot->GetVersion<BinTree>(v0)->right->AddVersion(new BinTree(7),v0);

    BinTree* v1bt=pRoot->GetVersion<BinTree>(v1)->left->GetVersion<BinTree>(v1);
    BinTree* v1bt_copy=new BinTree();
    *v1bt_copy=*v1bt;
    v1bt_copy->val=3;

    cVersion* v3=pRoot->GetVersion<BinTree>(v1)->left->AddVersion(v1bt_copy,v1);

    cVersion* v4=pRoot->GetVersion<BinTree>(v2)->left->AddVersion(new BinTree(3),v2);

    //destructive update of version 4
    pRoot->GetVersion<BinTree>(v4)->left->GetVersion<BinTree>(v4)->val=4;

    //test copy:


    PersistenceRoot root2;
    cVersion* v0c=root2.getCurVersionBit();

    //root2.SetFCT(pRoot->getRoot()->getNewCopyTag()); //would pass this into copy but it skews he timing!!
    
    PersistenceNode* pRoot2=pRoot->copy(&root2,v1,pRoot->getRoot()->getNewCopyTag());//(deep)copy v1 of pRoot into pRoot2

//    root2.SetRootNode(pRoot2);

    pRoot2->GetVersion<BinTree>(v0c)->val=50;
    pRoot2->GetVersion<BinTree>(v0c)->left->GetVersion<BinTree>(v0c)->val=20;
    pRoot2->GetVersion<BinTree>(v0c)->left->GetVersion<BinTree>(v0c)->left->GetVersion<BinTree>(v0c)->val=80;
    pRoot2->GetVersion<BinTree>(v0c)->right->GetVersion<BinTree>(v0c)->val=70;


    //test performance:

    cVersion* versions[]={v0,v1,v2,v3,v4};
    uint64 size=5;

    NPBinTree* pNPRoot=new NPBinTree(5,new NPBinTree(2),new NPBinTree(6));
    
    unsigned int sum=0;

    printf ("timing %ld access to DS\n",LOOP);
//    cVersion* version=versions[2];

    unsigned int sum2=0;

    //clock_t start2 = clock();
    timespec start2;
    get_time(start2);
    //if(false)
    NPBinTree* pNBT=pNPRoot;
    for(uint64 i=0;i<LOOP;i++)
    {
        //cVersion* version=versions[i%(size-1)];

        if(i%2==0)
        {
            sum2+=pNBT->left->val;
        }
        else
        {
            sum2+=pNBT->right->val;
        }

        //sum2+=version->mVersion.mSize;
    }

    timespec end2;
    get_time(end2);
    double delta2=diff_sec(start2,end2);
    printf ( "non-persistent access:%fs\n", delta2);
    printf("sum : %d\n",sum2);


//    clock_t start3 = clock();

    //create single version tree:
    PersistenceRoot root3;

    cVersion* v03=root3.getCurVersionBit();

    PersistenceNode *pRoot3=new PersistenceNode(new BinTree(66),v03,&root3);

    pRoot3->GetVersion<BinTree>(v03)->left=new PersistenceNode(new BinTree(11),v03,&root3);
    pRoot3->GetVersion<BinTree>(v03)->right=new PersistenceNode(new BinTree(99),v03,&root3);

    timespec start3;
    get_time(start3);

    BinTree* pBT=pRoot3->GetVersion<BinTree>(v03);
    //if(false)
    for(uint64 i=0;i<LOOP;i++)
    {
        //int index=(int)((float)rand()/(float)RAND_MAX)*(versions.size()-1);
        //cVersion* version=versions[i%(size-1)];

        if(i%2==0)
        {
            sum+=pBT->left->GetVersion<BinTree>(v03)->val;
        }
        else
        {
            sum+=pBT->right->GetVersion<BinTree>(v03)->val;
        }

        //sum+=version->mVersion.mSize;
    }
    timespec end3;
    get_time(end3);
    double delta3=diff_sec(start3,end3);

//    double delta3=( (double)clock() - start3 ) / CLOCKS_PER_SEC;
    printf ( "persistent (ethermal) access:%fs\n", delta3 );
    printf("sum : %d\n",sum);


    printf("creating %d versions ",VERSIONS);

    cVersion* last;
    cVersion* middle;
    uint8 cc=0;
    last=v3;
    uint32 ver_size=0;
    cVersion* plast=NULL;
    //if(false)
    for(int i=0;i<VERSIONS;i++)
    {
        //cVersion* version=versions[i%(size-1)];
        if(i%2)
        {
            plast=pRoot->GetVersion<BinTree>(last)->left->AddVersion(new BinTree(i),last);
        }
        else
        {
            plast=pRoot->GetVersion<BinTree>(last)->right->AddVersion(new BinTree(i),last);
        }
        if(i%1000000==0)
            printf(".");
        if(i==VERSIONS/2)
            middle=last;

        ver_size+=last->mVersion.size()*sizeof(uint64)+sizeof(cVersion)+2*sizeof(VersionInfo);

        //DO NOT DELETE VERSIONS!!

        last=plast;
        cc++;
    }
    printf("done \n");


    //clock_t start = clock();
    timespec start;
    get_time(start);

    pBT=pRoot->GetVersion<BinTree>(middle);
    //if(false)
    for(uint64 i=0;i<LOOP;i++)
    {
        //int index=(int)((float)rand()/(float)RAND_MAX)*(versions.size()-1);
        //cVersion* version=versions[i%(size-1)];

        if(i%2==0)
        {
            sum+=pBT->left->GetVersion<BinTree>(middle)->val;
        }
        else
        {
            sum+=pBT->right->GetVersion<BinTree>(middle)->val;
        }

        //sum+=version->mVersion.mSize;
    }

    timespec end;
    get_time(end);
    double delta=diff_sec(start,end);

//    double delta=( (double)clock() - start ) / CLOCKS_PER_SEC;
    printf ( "persistent access:%fs\n", delta );
    printf("sum : %d\n",sum);


    printf ( "ratio (pers/non-pers) :%0.4f\n", delta/delta2 );
    printf ( "ratio (pers(et)/non-pers) :%0.4f\n", delta3/delta2 );
    printf ( "ratio (pers(non-et)/pers(et)) :%0.4f\n", delta/delta3 );

    printf("add memory /ethermal node: %d kb\n",(sizeof(PersistenceNode)+
            (pRoot->GetVersion<BinTree>(v0)->left->getVersionFieldCount()+pRoot->GetVersion<BinTree>(v0)->right->getVersionFieldCount())/2*sizeof(uint64))/1024
            );

    printf("add memory /node version: %d bit\n",(sizeof(PersistenceNode)+
            (pRoot->GetVersion<BinTree>(v0)->left->getVersionFieldCount()+pRoot->GetVersion<BinTree>(v0)->right->getVersionFieldCount())/2*64)/(VERSIONS/2)
            );

    printf("avrg size of version tag: %d b\n",ver_size/VERSIONS);

    fprintf(out,"digraph graphname {\n");

    pRoot->Dump(v0,0);

//    BinTree *pBT=pRoot->GetVersion<BinTree>(v0)->left->GetVersion<BinTree>(v0);

    pRoot->Dump(v1,0);

    
    pRoot->Dump(v2,0);
    
    pRoot->Dump(v3,0);
    pRoot->Dump(v4,0);


    pRoot2->Dump(v0c,1);
    
    fprintf(out,"}");

    //PersistenceNode node();

    fclose(out);
}

void narysearch()
{
    uint32 *values=(uint32*)malloc(sizeof(uint32)*SIZE);

    for(uint32 u=0;u<SIZE;u++)
        values[u]=2*u;

    timespec start_5ary;
    get_time(start_5ary);

    uint32 offs=0;
//    uint32 offs2=binaryFind(values,SIZE,2);
    for(uint32 u=0;u<LOOP;u++)
        offs+=fivearySearchSSE(values,SIZE,2);

    timespec end_5ary;
    get_time(end_5ary);
    double delta_5ary=diff_sec(start_5ary,end_5ary); 
    
    printf ( "5ary[%d]:%fs\n", offs,delta_5ary);

    get_time(start_5ary);

    for(uint32 u=0;u<LOOP;u++)
        offs+=binaryFind(values,SIZE,2);

    delta_5ary=diff_sec(start_5ary,end_5ary);

    get_time(end_5ary);
    delta_5ary=diff_sec(start_5ary,end_5ary);
    printf ( "2ary[%d]:%fs\n", offs,delta_5ary);

}

int main(int argc, char** argv) {
        
    out=fopen("out.dot","wb");
    
    cVersionPointer<BinTree>* cpRoot1= new  cVersionPointer<BinTree>(new BinTree(5));

    cpRoot1->Get()->left=cpRoot1->New(new BinTree(1));
    cpRoot1->Get()->right=cpRoot1->New(new BinTree(6));
    
    cVersionPointer<BinTree>* cpRoot2=cpRoot1->AddVersion(cpRoot1->Get()->left,new BinTree(2));

    cpRoot2->Get(cpRoot2->Get()->left)->left=cpRoot2->New(new BinTree(8));

    cVersionPointer<BinTree>* cpRoot3=cpRoot1->AddVersion(cpRoot1->Get()->right,new BinTree(2));

    BinTree* v1bt=cpRoot2->Get(cpRoot2->Get()->left);
    BinTree* v1bt_copy=new BinTree();
    *v1bt_copy=*v1bt;
    v1bt_copy->val=3;


    fprintf(out,"digraph graphname {\n");

    cpRoot1->Dump(0);
    cpRoot2->Dump(0);
    cpRoot3->Dump(1);    
    
    fprintf(out,"}");

    //PersistenceNode node();

    fclose(out);    

    return 0;
}


            //int offset2=__builtin_popcountl(bitoffset);

            //mmx variant:
            //int offset2=0;
            //uint8* p4u8=(uint8*)&bitoffset;
            /*
            int offset=
            BitLookup[p4u8[0]]+BitLookup[p4u8[1]]+BitLookup[p4u8[2]]+BitLookup[p4u8[3]];
            +
            BitLookup[p4u8[4]]+BitLookup[p4u8[5]]+BitLookup[p4u8[6]]+BitLookup[p4u8[7]];
            */
/*

            int offset;//=ssse3_popcount2((uint64*)&bitoffset);
#ifdef POPCNT
            __asm__ volatile(
                    "popcnt    %%rax, %%rax	\n"	// for all nibbles
                    : "=a" (offset)
                    : "a" (bitoffset)
            );

//            __asm__ volatile ("movdqu (%%eax), %%xmm7" : : "a" (POPCOUNT_4bit));
//            __asm__ volatile ("movdqu (%%eax), %%xmm6" : : "a" (MASK_4bit));
#else
            __asm__ volatile(
                    "movq      %%rax, %%mm0     \n"
                    "pand      %%mm6, %%mm0	\n"	// xmm0 := lower nibbles
                    "movq      %%mm7, %%mm2	\n"
                    "pshufb    %%mm0, %%mm2	\n"	// for all nibbles
                    "movd      %%mm2, %%eax     \n"
                    : "=a" (offset)
                    : "a" (bitoffset)
            );

#endif

*/
/*
            uint64 offset;
            __asm__ volatile(
                    "and       %2,%1      \n"
                    "bsr       %1,%1      \n"
                    "xor       $0x3f,%1      \n"
                    "mov       (%3,%1,8), %1\n"
                    "and       %2,%1      \n"
                    "popcnt    %1,%0      \n"

//                    "movq      %1, %%mm0     \n"
//                   "pand      %%mm6, %%mm0	\n"	// xmm0 := lower nibbles
//                    "movq      %%mm7, %%mm2	\n"
//                    "pshufb    %%mm0, %%mm2	\n"	// for all nibbles
//                    "movd      %%mm2, %0     \n"

                    : "=r" (offset)
                    : "r" (version),"r" (mAvilableVersions),"r" (BitMask)
            );
*/
/*
            if(offset!=offset2)
            {
                asm("int     $0x3");
            }
*/

/*
            uint64 offset;
            __asm__ volatile(
                    "and       %2,%1      \n"
                    "bsr       %1,%1      \n"
//                    "xor       $0x3f,%1      \n" //this is slow??
//                    "and       (%3,%1,8), %2\n" //this is slow??
                    "mov       (%3,%1,8), %1\n"
                    "and       %1, %2\n"
                    "popcnt    %2,%0      \n"

                    : "=r" (offset)
                    : "r" (version),"r" (mAvilableVersions),"r" (BitMask)
            );
 */


