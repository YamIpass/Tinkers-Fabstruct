import io

src = io.open('C:/Users/YamIpass/AppData/Local/Temp/cpt/opts.args', encoding='utf-8').read().splitlines()
cp = src[13]
out = ['--release', '17', '-proc:full', '-nowarn', '-Xmaxerrs', '200', '-implicit:none',
       '-sourcepath', 'src/main/java', '-d', 'C:/Users/YamIpass/AppData/Local/Temp/jm', '-cp', cp]
io.open('C:/Users/YamIpass/AppData/Local/Temp/cpt/opts_mine.args', 'w', encoding='utf-8', newline='\n').write('\n'.join(out) + '\n')
print('ok', len(cp))
