// @PARAM: -verbose -regression

@interface Serialize {} //@NOERROR(*)

class Packet
{
    public int data;
    @Serialize public int id;

    Packet(int data,int id)
    {
        boolean t=Packet.class.getField("id").isAnnotationPresent(Serialize.class); //@NOERROR(*)
        if(t)
        {
            int i=0;
        }
        this.data=data;
        this.id=id;
    }
}
