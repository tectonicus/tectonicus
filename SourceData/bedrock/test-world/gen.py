"""Builds the synthetic Bedrock world in src/test/resources/bedrock/world.

    mkdir world && python3 gen.py world      # writes level.dat, levelname.txt and records.json
    npm install classic-level@1 && node write.js world/db

gen.py encodes the chunk records (sub-chunks, Data3D, block entities) as Bedrock stores them; write.js puts them
in a LevelDB database and compacts it into a table file. Copy level.dat, levelname.txt and db/ into the fixture.
"""
import json, struct, sys

def s(v): b=v.encode(); return struct.pack('<H',len(b))+b
def tag(t,name,payload): return bytes([t])+s(name)+payload
def compound(name, items):  # items: list of (type, name, payload)
    return tag(10,name,b''.join(tag(t,n,p) for t,n,p in items)+b'\x00')
def cpayload(items): return b''.join(tag(t,n,p) for t,n,p in items)+b'\x00'
def i32(v): return struct.pack('<i',v)
def i64(v): return struct.pack('<q',v)
def strp(v): return s(v)

def block(name, states):
    st=[]
    for k,v in states.items():
        if isinstance(v,bool): st.append((1,k,bytes([1 if v else 0])))
        elif isinstance(v,int): st.append((3,k,i32(v)))
        else: st.append((8,k,strp(v)))
    return compound('', [(8,'name',strp(name)),(10,'states',cpayload(st)),(3,'version',i32(18168865))])

def storage(fn):
    """fn(x,y,z)->(name,states) ; returns bytes of one layer"""
    palette=[]; idx=[]
    keys={}
    for x in range(16):
        for z in range(16):
            for y in range(16):
                b=fn(x,y,z); k=json.dumps(b,sort_keys=True)
                if k not in keys: keys[k]=len(palette); palette.append(b)
                idx.append(keys[k])
    n=len(palette)
    if n==1:
        out=bytes([0<<1])
        out+=b''  # bits 0: no words, no count
        return out+block(*palette[0])
    bits=next(b for b in (1,2,3,4,5,6,8,16) if (1<<b)>=n)
    per=32//bits; words=(4096+per-1)//per
    out=bytearray([bits<<1])
    for w in range(words):
        word=0
        for j in range(per):
            i=w*per+j
            if i<4096: word|=idx[i]<<(j*bits)
        out+=struct.pack('<I',word)
    out+=i32(n)
    for b in palette: out+=block(*b)
    return bytes(out)

def key(cx,cz,tagb,sub=None,dim=0):
    k=struct.pack('<ii',cx,cz)+(struct.pack('<i',dim) if dim else b'')+bytes([tagb])
    if sub is not None: k+=struct.pack('<b',sub)
    return k

AIR=('minecraft:air',{})
recs={}
def put(k,v): recs[k.hex()]=v.hex()

for cx in range(-2,2):
    for cz in range(-2,2):
        put(key(cx,cz,0x2c), bytes([40]))
        put(key(cx,cz,0x36), i32(2))
        # terrain by world y: -64 bedrock, < 60 stone, 60 dirt, 61 grass, water pool in chunk (0,0)
        def at(wx,wy,wz):
            if wy==-64: return ('minecraft:bedrock',{'infiniburn_bit':False})
            if wy<60: return ('minecraft:stone',{})
            if wy==60: return ('minecraft:dirt',{'dirt_type':'normal'})
            if wy==61:
                if 0<=wx<6 and 0<=wz<6: return ('minecraft:water',{'liquid_depth':0})
                return ('minecraft:grass_block',{})
            if wy==62 and wx==8 and wz==8: return ('minecraft:standing_sign',{'ground_sign_direction':0})
            if wy==62 and wz==10 and wx in (9,10): return ('minecraft:chest',{'minecraft:cardinal_direction':'south'})
            if wy==62 and wx in range(12,16) and wz==2: return ('minecraft:oak_fence',{})
            if wy in range(62,67) and wx==-5 and wz==-5: return ('minecraft:oak_log',{'pillar_axis':'y'})
            if wy in range(66,69) and abs(wx+5)<=2 and abs(wz+5)<=2 and not (wx==-5 and wz==-5 and wy<67): return ('minecraft:oak_leaves',{'persistent_bit':False,'update_bit':False})
            if wy==62 and wx==3 and wz==3: return ('minecraft:oak_fence',{})
            return AIR
        def water(wx,wy,wz):
            if wy==62 and wx==3 and wz==3: return ('minecraft:water',{'liquid_depth':0})
            return AIR
        for sub in range(-4,5):
            def f(x,y,z,sub=sub): return at(cx*16+x, sub*16+y, cz*16+z)
            def g(x,y,z,sub=sub): return water(cx*16+x, sub*16+y, cz*16+z)
            layer0=storage(f)
            if sub==3 and cx==0 and cz==0:
                data=bytes([9,2,sub&0xff])+layer0+storage(g)
            else:
                data=bytes([9,1,sub&0xff])+layer0
            put(key(cx,cz,0x2f,sub), data)
        # Data3D: heightmap + biomes: plains(1) normally, swamp(6) in chunk (-1,-1), cherry (192) in (1,1)
        biome={(-1,-1):6,(1,1):192}.get((cx,cz),1)
        d3=b'\x00'*512 + bytes([0]) + i32(biome) + bytes([0xff])*23
        put(key(cx,cz,0x2b), d3)
        if cx==0 and cz==0:
            be=compound('',[(8,'id',strp('Sign')),(3,'x',i32(8)),(3,'y',i32(62)),(3,'z',i32(8)),
                (10,'FrontText',cpayload([(8,'Text',strp('Hello\nBedrock\n§cWorld')),(3,'SignTextColor',i32(-16777216)),(1,'IgnoreLighting',b'\x00')]))])
            be+=compound('',[(8,'id',strp('Chest')),(3,'x',i32(9)),(3,'y',i32(62)),(3,'z',i32(10)),(3,'pairx',i32(10)),(3,'pairz',i32(10))])
            be+=compound('',[(8,'id',strp('Chest')),(3,'x',i32(10)),(3,'y',i32(62)),(3,'z',i32(10)),(3,'pairx',i32(9)),(3,'pairz',i32(10))])
            put(key(cx,cz,0x31), be)
# an unfinished chunk that must be skipped
put(key(5,5,0x2c), bytes([40])); put(key(5,5,0x36), i32(1))
# a nether chunk
put(key(0,0,0x2c,dim=1), bytes([40]))
put(key(0,0,0x2f,0,dim=1), bytes([9,1,0])+storage(lambda x,y,z: ('minecraft:netherrack',{}) if y<8 else AIR))
put(b'~local_player'.ljust(13,b'\0')[:13], b'')

lvl=cpayload([(8,'LevelName',strp('Synthetic Bedrock')),(3,'SpawnX',i32(0)),(3,'SpawnY',i32(32767)),(3,'SpawnZ',i32(0)),
    (4,'RandomSeed',i64(12345)),(3,'StorageVersion',i32(10)),
    (9,'lastOpenedWithVersion',bytes([3])+i32(5)+i32(1)+i32(21)+i32(120)+i32(4)+i32(0))])
lvl=bytes([10])+s('')+lvl
open(sys.argv[1]+'/level.dat','wb').write(i32(10)+i32(len(lvl))+lvl)
open(sys.argv[1]+'/levelname.txt','w').write('Synthetic Bedrock')
json.dump(recs, open('records.json','w'))
print(len(recs),'records')
