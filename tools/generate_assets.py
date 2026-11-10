#!/usr/bin/env python3
"""Reproducible original 16x16 pixel art and 44.1kHz stereo sound design.
Optional build-time dependencies: Pillow and numpy. Neither is needed to play.
"""
from pathlib import Path
from PIL import Image, ImageDraw
import numpy as np
import math, random, wave
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
SPR=ROOT/'sprites'; AUD=ROOT/'audio'
SPR.mkdir(parents=True,exist_ok=True); AUD.mkdir(parents=True,exist_ok=True)
rng=np.random.default_rng(707); random.seed(707)
N=getattr(Image,'Resampling',Image).NEAREST
patterns={
'player':[
'......oooo......','.....ohhhho.....','.....ohhhho.....','.....ohvvho.....',
'......ohho......','....ookkkkoo....','...ohhkkkhhho...','...ohhkkkhhho...',
'...ohokkkkohho..','...obokkkkohbo..','....ookkkkoo....','.....ohhho......',
'.....ohhho......','.....oh. ho......'.replace(' ',''),'.....ok..ko.....','.....oo..oo.....'],
'sentinel':[
'......oooo......','.....ommmmo.....','.....ommmmo.....','.....orrrro.....',
'......ommo......','....oommmmoo....','...ommummmumo...','..oommummmumoo..',
'..omoommmmoomo..','..omoommmmoomo..','..omoommmmoomo..','...ooommmmooo...',
'.....ommmo......','.....omomo......','....omm.ommo....','....ooo.oooo....'],
'drone':[
'................','..ooo......ooo..','..ommmo..ommmo..','..ooooo..ooooo..',
'....oommmmoo....','.....ommmmo.....','....ommmmmo.....','...ommrrrmmo....',
'...ommrrrmmo....','....ommmmmo.....','.....ommmmo.....','....oommmmoo....',
'..ooooo..ooooo..','..ommmo..ommmo..','..ooo......ooo..','................'],
'specimen':[
'.......ooo......','......obboo.....','.....obbbbo.....','.....oboboo.....',
'.....obrrbo.....','......obbo......','....oobbbboo....','...obobbbobbo...',
'..obboobobobbo..','..obooobobooobo.','..obo.obbbo.obo.', '..obo..obbo..obo',
'.obo...oboo..obo','.oo...obobo...oo','......ob.obo....','.....ooo..ooo...']}
palette={'o':'#0c1013','h':'#87938d','v':'#b3c7b0','k':'#343f43','b':'#a99486','m':'#586269','r':'#d45745','u':'#9c8d72'}
for name,rows in patterns.items():
    assert len(rows)==16,(name,len(rows))
    rows=[r.ljust(16,'.')[:16] for r in rows]
    sheet=Image.new('RGBA',(64,64))
    for direction in range(4):
        for frame in range(4):
            im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
            for y,row in enumerate(rows):
                for x,c in enumerate(row):
                    if c=='.':continue
                    yy=y
                    if frame in (1,3) and y>=12 and name!='drone':yy=max(0,min(15,y+(1 if x<8 else -1)*(1 if frame==1 else -1)))
                    d.point((x,yy),fill=palette[c])
            if direction==1: im=im.transpose(Image.Transpose.FLIP_LEFT_RIGHT) if hasattr(Image,'Transpose') else im.transpose(Image.FLIP_LEFT_RIGHT)
            d=ImageDraw.Draw(im)
            if name in ('player','sentinel'):
                if direction==3:
                    d.rectangle((6,2,9,4),fill='#525b58');d.rectangle((6,6,9,10),fill='#4c514c')
                elif direction in (1,2):
                    d.rectangle((6,3,9,3),fill='#525b58');d.point((6 if direction==1 else 9,3),fill='#b4bba5' if name=='player' else '#ed775b')
            if name=='specimen' and direction==3:
                d.rectangle((6,2,9,4),fill='#817d71');d.line((7,6,7,11),fill='#c1b49c')
            if name=='drone' and frame%2:d.line((2,2,4,2),fill='#9dabad');d.line((11,13,13,13),fill='#9dabad')
            sheet.alpha_composite(im,(frame*16,direction*16))
    sheet.save(SPR/f'{name}.png')

# Agent 07: red field jacket, dark hair, skin and boots. Four hand-authored views.
sheet=Image.new('RGBA',(64,64))
for direction in range(4):
    for frame in range(4):
        im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
        swing=[0,1,0,-1][frame]
        def r(b,c):d.rectangle(b,fill=c)
        edge='#111622';red='#af4249';light='#df6b62';skin='#e1b98e';pants='#30465c'
        # Boots and trousers are separated even at native resolution.
        for x,offset in [(5,swing),(9,-swing)]:
            r((x,11,x+2,14),edge);r((x,11,x+1,13),pants);r((x,14+min(0,offset),x+2,14+min(0,offset)),edge)
        r((4,7,11,11),edge);r((5,7,10,11),red);r((5,7,5,9),light)
        r((7,7,8,9),'#e0d1ae');r((7,10,8,11),'#633442');r((5,11,10,11),'#242e3d')
        # Separate arm silhouettes with gloved hands.
        r((3,7+max(0,swing),4,10+max(0,swing)),edge);r((4,7+max(0,swing),4,9+max(0,swing)),light)
        r((11,7+max(0,-swing),12,10+max(0,-swing)),edge);r((11,7+max(0,-swing),11,9+max(0,-swing)),red)
        r((4,10+max(0,swing),4,11+max(0,swing)),'#c49b72');r((11,10+max(0,-swing),11,11+max(0,-swing)),skin)
        # Head: angular dark hair, warm skin, side lighting, no helmet.
        r((5,1,10,5),edge);r((6,0,9,1),edge);r((5,1,10,2),'#29313b')
        r((6,3,10,5),skin);r((6,3,6,4),'#b78365');r((7,5,9,6),'#ca9872')
        if direction==0:
            r((7,3,7,3),edge);r((10,3,10,3),edge);r((8,5,9,5),'#996a56')
        elif direction==3:
            r((5,2,10,5),'#202c38');r((6,2,9,2),'#3b4550');r((7,6,8,6),'#242d39')
            r((6,7,9,10),'#354658');r((7,7,8,9),'#8c7c59');r((6,10,9,10),'#1a293a')
        else:
            r((6,3,10,4),'#29313b');r((9,3,11,4),skin);r((10,3,10,3),edge);r((9,5,10,5),'#ca9872')
            r((6,7,7,10),'#6e333e');r((8,8,10,9),light)
            if direction==1:im=im.transpose(Image.Transpose.FLIP_LEFT_RIGHT) if hasattr(Image,'Transpose') else im.transpose(Image.FLIP_LEFT_RIGHT)
        sheet.alpha_composite(im,(frame*16,direction*16))
sheet.save(SPR/'player.png')
# A small field-ID portrait; not a generated face pasted over the game.
portrait=Image.new('RGBA',(32,32),'#1c2a37');d=ImageDraw.Draw(portrait)
d.polygon([(2,31),(4,24),(11,21),(20,21),(28,24),(31,31)],fill='#ad454b')
d.polygon([(11,23),(16,29),(21,23),(20,31),(11,31)],fill='#dbcfb4')
d.rectangle((13,18,19,24),fill='#c18c68');d.polygon([(9,7),(12,3),(21,4),(24,9),(22,19),(18,23),(12,20),(9,14)],fill='#e0b48a')
d.polygon([(8,12),(7,8),(10,3),(16,1),(23,4),(25,8),(23,13),(21,9),(17,7),(11,9)],fill='#172434')
d.rectangle((11,12,13,12),fill='#252b35');d.rectangle((19,12,21,12),fill='#252b35')
d.line([(16,13),(15,17),(17,17)],fill='#b47b5f');d.line((14,20,18,20),fill='#825747')
d.line((22,15,21,19),fill='#bf8e6b');d.line((10,24,8,30),fill='#d96c5b')
portrait.save(SPR/'portrait.png')
icons=Image.new('RGBA',(64,16));d=ImageDraw.Draw(icons)
d.polygon([(2,5),(4,3),(6,3),(8,5),(10,3),(12,3),(14,5),(14,8),(8,14),(2,8)],fill='#e3d6bd')
d.rectangle((7,6,8,10),fill='#b24748');d.rectangle((5,7,10,8),fill='#b24748')
d.rectangle((18,5,29,7),fill='#ddd3bc');d.rectangle((26,7,28,9),fill='#ddd3bc');d.polygon([(21,8),(25,8),(24,13),(20,13)],fill='#b78d68')
d.ellipse((35,3,46,14),outline='#c3d5d2',width=1);d.line([(41,3),(38,9),(42,9),(39,14)],fill='#e5dec6',width=2)
d.polygon([(54,2),(60,2),(56,7),(61,7),(53,15),(55,9),(51,9)],fill='#dbc79d')
icons.save(SPR/'icons.png')

# 24 original tiles, each exactly 16px. Extra tiles include dynamic security doors.
atlas=Image.new('RGBA',(128,48))
for i in range(24):
    im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
    def r(box,c):d.rectangle(box,fill=c)
    if i<4:
        r((0,0,15,15),'#242d38' if i!=1 else '#27313e')
        r((0,0,15,0),'#303b48');r((0,0,0,15),'#303946');r((1,15,15,15),'#1d2225')
        for _ in range(7):
            x,y=random.randrange(2,15),random.randrange(2,15);d.point((x,y),fill='#37414b')
        if i==2:
            for _ in range(24):d.point((random.randrange(1,15),random.randrange(1,15)),fill='#554b37')
        if i==3:
            d.ellipse((1,4,14,13),fill='#3e4933');d.ellipse((4,5,12,12),fill='#59613d')
            for p in [(6,7),(11,9),(8,11)]:d.point(p,fill='#a8ad69')
    elif i==4:
        r((0,0,15,15),'#11191e');r((0,0,15,10),'#4b5b69');r((1,1,14,2),'#89938f');r((1,4,14,9),'#3c474b');r((1,11,14,13),'#263136')
        d.line((3,4,3,8),fill='#626a63');d.line((12,4,12,8),fill='#242e34')
    elif i==5:
        r((0,3,15,12),'#101a1e');r((1,1,14,10),'#737d76');r((2,2,13,3),'#a7afa0');r((2,5,13,9),'#414e50')
        r((3,5,7,8),'#172a2a');r((4,6,6,7),'#889a76');r((9,5,12,6),'#8c7760');r((2,11,3,14),'#293538');r((12,11,13,14),'#293538')
    elif i in (6,7,18):
        r((3,1,12,14),'#101a1e');r((4,0,11,12),'#68736e');r((5,2,10,7),'#0c1c21');r((6,3,9,6),'#717f5c' if i==7 else '#34514c')
        d.line((6,4,9,4),fill='#b3c38d');r((5,9,10,10),'#252c2c');d.point((6,9),fill='#b17a51')
    elif i==8:
        r((1,2,14,14),'#172529');r((2,1,13,12),'#667770');r((3,2,12,3),'#99a594');r((3,5,12,10),'#263a35')
        for x in (5,8,11):r((x,6,x,9),'#bc9b64')
    elif i==9:
        r((0,0,15,15),'#1a272c');r((1,0,14,14),'#65716c');r((3,2,12,14),'#293c36');r((7,3,8,14),'#89957a');r((2,0,13,1),'#b6bb91')
    elif i==10:
        r((3,5,12,12),'#434e4b');r((4,4,11,10),'#b3b6a0');r((7,5,8,9),'#a35e4f');r((5,6,10,7),'#a35e4f')
    elif i==11:
        r((2,3,13,13),'#1d292b');r((2,1,13,11),'#7e7960');r((3,2,12,3),'#a69b78');r((4,4,11,10),'#575943');r((7,4,8,11),'#383e35')
    elif i==12:
        r((3,1,12,14),'#333c3c');d.ellipse((4,0,11,14),fill='#526c5b');r((5,2,10,11),'#2b413b')
        r((6,4,9,10),'#7c8570');r((5,1,10,2),'#97a38b');r((4,12,11,13),'#828b7b')
    elif i==13:
        for y in (3,7,11):r((1,y,14,y+1),'#111b1f');d.line((2,y,13,y),fill='#4f5954')
    elif i==14:
        r((0,6,15,8),'#526463');r((0,6,15,6),'#8e9b88');r((7,0,9,15),'#526463')
    elif i==15:
        r((1,6,14,9),'#252a29')
        for x in range(1,14,3):r((x,6,x+1,9),'#9d8454')
    elif i==16:
        r((0,0,15,15),'#161f24');r((2,0,13,15),'#626966');r((3,1,6,14),'#3b4546');r((9,1,12,14),'#3b4546');r((7,0,8,15),'#a66b55')
        for y in (3,7,11):r((4,y,5,y),'#919587');r((10,y,11,y),'#919587')
    elif i==17:
        r((0,0,2,15),'#5c6460');r((13,0,15,15),'#5c6460');r((1,1,1,14),'#a69366');r((14,1,14,14),'#a69366')
    elif i==19:
        for p in [(7,6),(8,7),(5,9),(10,8)]:d.point(p,fill='#8c5d4b')
    elif i==20:
        d.ellipse((1,7,13,13),fill='#4b3430');r((5,6,11,10),'#69706a');r((7,4,9,6),'#505956');r((10,4,14,6),'#333e40');r((11,10,15,12),'#333e40');r((4,10,7,12),'#7b7f6c');r((1,6,4,8),'#aba28c');d.point((2,7),fill='#51463d')
    elif i==21:
        d.line([(1,14),(6,10),(5,5),(12,1)],fill='#0c1519',width=1);d.point((6,10),fill='#64665b')
    elif i==22:
        r((3,1,12,13),'#404b4c');r((4,2,11,11),'#64716c');r((5,3,10,8),'#1b292e');r((6,5,9,5),'#ba624c')
    elif i==23:
        r((1,1,14,14),'#30352e');d.line((2,2,13,13),fill='#9c9d73');d.line((13,2,2,13),fill='#9c9d73')
    atlas.alpha_composite(im,((i%8)*16,(i//8)*16))
atlas.save(SPR/'facility.png')
# Derelict containment illustration, not a marketing mascot.
hero=Image.new('RGBA',(192,200));d=ImageDraw.Draw(hero)
def r(b,c):d.rectangle(b,fill=c)
for x in (30,153):
    r((x,20,x+6,178),'#263337');r((x+1,20,x+2,172),'#66716a')
    for y in range(28,170,16):r((x+1,y,x+5,y+1),'#929782')
d.line([(34,32),(44,32),(44,16),(147,16),(147,48),(156,48)],fill='#626e67',width=3)
r((51,38,142,155),'#223533');r((57,43,136,152),'#30453c');r((63,45,130,150),'#35483c')
# Gaunt suspended body, exposed ribs and long hands.
d.polygon([(86,64),(91,58),(105,61),(110,70),(105,87),(94,92),(85,79)],fill='#202b29');d.polygon([(90,64),(95,61),(104,65),(106,72),(102,85),(95,88),(89,78)],fill='#92917c')
r((91,70,95,73),'#162021');r((100,71,104,74),'#162021');d.point((102,72),fill='#b96d53');r((95,78,100,81),'#392c27');d.line((93,83,101,82),fill='#514c3c')
r((91,85,102,93),'#707b68');d.polygon([(84,94),(108,94),(112,123),(102,137),(87,130),(78,111)],fill='#5f6d5c')
for y in range(99,122,5):d.line([(84,y),(94,y+3),(107,y)],fill='#b0ab8d',width=2)
d.line((96,95,96,130),fill='#c1b598',width=2)
d.line([(81,96),(70,111),(68,135),(61,143)],fill='#747e67',width=4)
d.line([(109,96),(120,112),(122,139),(131,145)],fill='#747e67',width=4)
for x in range(61,67,2):d.line((x,139,x-2,149),fill='#c0b299')
for x in range(127,134,2):d.line((x,141,x+1,150),fill='#c0b299')
d.line([(89,128),(83,143),(87,153)],fill='#9d9b80',width=4);d.line([(103,130),(110,143),(104,153)],fill='#9d9b80',width=4)
# Glass highlights, grime and cracks.
d.line((60,46,60,130),fill='#7b8d75');d.line((132,48,132,127),fill='#607362')
d.line([(120,47),(112,73),(119,88),(109,105),(115,121)],fill='#98a58c')
d.line([(119,88),(131,86)],fill='#768e7d')
for y in (33,149):
    d.ellipse((48,y,145,y+22),fill='#293436');r((50,y+8,143,y+17),'#6d7770')
    d.ellipse((51,y,142,y+14),fill='#8b9181');d.ellipse((58,y+3,135,y+11),fill='#35433d')
    r((52,y+13,141,y+16),'#3a4543')
    for x in (57,78,113,133):r((x,y+12,x+3,y+19),'#a88557')
r((22,170,173,181),'#2c3737');r((24,169,170,171),'#74816e');r((58,174,133,177),'#8a7854')
for x in range(60,132,8):r((x,174,x+3,177),'#333e36')
r((151,94,180,145),'#233032');r((153,96,178,140),'#616f68');r((155,99,176,121),'#15292a')
d.line([(157,112),(161,112),(163,106),(166,116),(169,108),(174,108)],fill='#a4ab81')
for y in (127,132,137):r((156,y,173,y+1),'#99a28a')
r((43,61,47,76),'#ad6c51');hero.save(SPR/'title.png')
catalog=Image.new('RGB',(512,320),'#161e20')
catalog.paste(atlas.resize((512,192),N),(0,0),atlas.resize((512,192),N))
for i,name in enumerate(patterns):
    pic=Image.open(SPR/f'{name}.png').resize((128,128),N);catalog.paste(pic,(i*128,192),pic)
catalog.save(SPR/'catalog.png')

RATE=44100
TAU=2*np.pi

def noise(n,cut=800):
    raw=rng.normal(0,1,n);f=np.fft.rfftfreq(n,1/RATE)
    return np.fft.irfft(np.fft.rfft(raw)/(1+(f/cut)**4),n=n)
def verb(x,wet=.2):
    out=x.copy()
    for delay,gain in [(.019,.35),(.043,.28),(.089,.2),(.137,.12),(.211,.08)]:
        shift=int(delay*RATE)
        if shift<len(x):out[shift:]+=x[:-shift]*gain*wet
    return out

def save(name,x,loop=False):
    if x.ndim==1:
        right=np.roll(x,int(.0037*RATE))*.98
        x=np.column_stack([x,right])
    # Remove DC, preserve dynamics, peak normalize and leave headroom.
    x=x-x.mean(axis=0)
    peak=np.max(np.abs(x));x=x/max(peak,1e-6)*(.68 if not loop else .42)
    fade=min(int(RATE*(.08 if loop else .005)),len(x)//4)
    x[:fade]*=np.linspace(0,1,fade)[:,None];x[-fade:]*=np.linspace(1,0,fade)[:,None]
    with wave.open(str(AUD/f'{name}.wav'),'wb') as w:
        w.setnchannels(2);w.setsampwidth(2);w.setframerate(RATE);w.writeframes((x*32767).astype('<i2').tobytes())

# 48-second evolving dissonant room tone; no upbeat tune, clicks or sharp loop seam.
t=np.arange(RATE*48)/RATE
left=np.zeros(len(t));right=np.zeros(len(t))
for f,a in [(36.708,.2),(55,.14),(73.416,.1),(110,.045),(155.563,.025),(220,.012)]:
    left+=a*np.sin(TAU*f*t+.11*np.sin(TAU*t/12))
    right+=a*np.sin(TAU*(f+.035)*t+.12*np.sin(TAU*t/16))
texture=noise(len(t),460)*.18
left+=texture*(.5+.5*np.sin(TAU*t/24)**2);right+=np.roll(texture,4410)*.7
for start in (6,19,34):
    env=np.exp(-((t-start)/2.5)**2);left+=.025*env*np.sin(TAU*196*t);right+=.023*env*np.sin(TAU*207.65*t)
save('ambient',np.column_stack([left,right]),True)
t=np.arange(RATE*16)/RATE
machine=.11*np.sin(TAU*49*t)+.05*np.sin(TAU*98*t)+noise(len(t),1600)*.2
machine*=.75+.2*np.sin(TAU*.25*t)
save('machinery',machine,True)
for name,seconds in [('heartbeat',2),('breath',4),('step',.3),('step2',.32),('step3',.29),('sneak',.28),('terminal',1.1),('pulse',1.5),('lure',1.3),('hit',.7),('win',3),('lose',2.5),('door',1.5),('radio',.9),('alert',1.1),('roar',1.4),('interact',.25),('drip',1.1)]:
    t=np.arange(int(RATE*seconds))/RATE;n=noise(len(t),2200);x=np.zeros(len(t))
    if name=='heartbeat':
        for start,amp in [(0.12,1),(.34,.6),(1.12,.8),(1.34,.5)]:
            u=np.maximum(0,t-start);x+=amp*np.sin(TAU*52*u)*np.exp(-u*23)*(t>=start)
    elif name=='breath':x=noise(len(t),750)*np.maximum(0,np.sin(TAU*t/4))**1.7
    elif name.startswith('step') or name=='sneak':
        f=82+random.randrange(-10,10);x=(.4*np.sin(TAU*f*t)+n)*np.exp(-t*28)
        x+=.22*noise(len(t),4800)*np.exp(-np.maximum(0,t-.045)*44)*(t>.045)
        x=verb(x,.45)
        if name=='sneak':x=noise(len(t),500)*np.exp(-t*18)*.7
    elif name=='pulse':
        x=.8*np.sin(TAU*(65*t-12*t*t))*np.exp(-t*5)+noise(len(t),6500)*np.exp(-t*11)
        x+=.15*np.sin(TAU*1250*t)*np.exp(-t*8);x=verb(x,.8)
    elif name=='terminal':
        x=np.sin(TAU*(480*t+150*t*t))*.25*np.exp(-t*4)+n*.35*np.exp(-t*16)
        for start,f in [(.28,660),(.42,880),(.62,550)]:x+=(t>=start)*np.sin(TAU*f*t)*np.exp(-np.maximum(0,t-start)*14)*.25
        x=verb(x,.55)
    elif name=='lure':x=(np.sin(TAU*1150*t)+.25*np.sin(TAU*1535*t))*np.exp(-((t%.26)/.045)**2)*np.exp(-t*.6)*.35;x=verb(x,.4)
    elif name=='hit':x=noise(len(t),900)*np.exp(-t*13)+.7*np.sin(TAU*(72*t-18*t*t))*np.exp(-t*11);x=verb(x,.3)
    elif name=='door':
        x=noise(len(t),550)*np.sin(np.pi*t/seconds)**1.5+.3*np.sin(TAU*(78*t+10*t*t))*np.sin(np.pi*t/seconds)
        x+=(t>1.17)*noise(len(t),4000)*np.exp(-np.maximum(0,t-1.17)*40);x=verb(x,.8)
    elif name=='radio':
        x=noise(len(t),4000)*.6*np.exp(-t*8)
        x+=.12*np.sin(TAU*1400*t)*(t>.2)*(t<.35)+.1*np.sin(TAU*1000*t)*(t>.42)*(t<.6)
    elif name=='alert':x=np.sin(TAU*(420*t+90*t*t))*.3*np.exp(-t*2)+noise(len(t),1300)*np.exp(-t*12);x=verb(x,.6)
    elif name=='roar':x=np.tanh(4*np.sin(TAU*(63*t-8*t*t)))*.3+noise(len(t),1100)*.45;x*=np.sin(np.pi*t/seconds)**1.3;x=verb(x,.7)
    elif name=='interact':x=n*.4*np.exp(-t*35)+.12*np.sin(TAU*740*t)*np.exp(-t*25)
    elif name=='drip':x=np.sin(TAU*(1800*t-600*t*t))*np.exp(-t*14)+n*.15*np.exp(-t*28);x=verb(x,1)
    elif name=='win':x=(np.sin(TAU*146.832*t)*.3+np.sin(TAU*220*t)*.16+noise(len(t),500)*.15)*np.sin(np.pi*t/seconds)**1.6
    elif name=='lose':x=(np.sin(TAU*(66*t-8*t*t))*.5+noise(len(t),600)*.3)*np.exp(-t*1.8);x=verb(x,.7)
    save(name,x)


# Short weapon and containment transients, using the same PCM/reverb pipeline.
for name,seconds in [('shot',.5),('ricochet',.45),('breach',2.3)]:
    t=np.arange(int(RATE*seconds))/RATE
    if name=='shot':x=noise(len(t),6500)*np.exp(-t*44)+.65*np.sin(TAU*98*t)*np.exp(-t*23);x=verb(x,.8)
    elif name=='ricochet':x=(np.sin(TAU*2400*t)+.4*np.sin(TAU*3170*t))*np.exp(-t*20);x=verb(x,.6)
    else:x=noise(len(t),750)*np.exp(-t*1.8)+.55*np.sin(TAU*(85*t-8*t*t))*np.sin(np.pi*t/seconds);x=verb(x,.8)
    save(name,x)

print('16×16 characters and tiles, portrait, icons; 23 stereo 44.1kHz sounds.')
