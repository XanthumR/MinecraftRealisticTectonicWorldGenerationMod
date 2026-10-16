# Debug aid: prints ground/water after every assignment in Terrain.column() for the column at -Dtx -Dtz (-Dtrace=true).
# Run on a copy; restore Terrain.java afterwards. Line numbers refer to the uninstrumented file.
import re
p='src/main/java/com/realtectonics/sim/Terrain.java'; L=open(p,encoding='utf-8').read().split('\n')
start=[i for i,l in enumerate(L) if 'private Column column(double x, double z, boolean ponds)' in l][0]
end=[i for i,l in enumerate(L) if i>start and 'return new Column(ground, water, lava, ice, biome, fold, volcanic, rock, cover);' in l][0]
out=[]
for i,l in enumerate(L):
    out.append(l)
    if start<i<end and re.search(r'\b(ground|water|ry)\s*(=|\+=|-=)', l) and l.rstrip().endswith(';') and 'for (' not in l:
        out.append('        if (TRACE && Math.abs(x - Integer.getInteger("tx")) < 0.5 && Math.abs(z - Integer.getInteger("tz")) < 0.5) System.out.println("L%d ground " + ground + " water " + water);' % (i+1))
s='\n'.join(out)
s=s.replace('    private final boolean simCalderas;','    static final boolean TRACE = Boolean.getBoolean("trace");\n    private final boolean simCalderas;',1)
open(p,'w',encoding='utf-8').write(s)
