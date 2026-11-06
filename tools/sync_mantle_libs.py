"""Swap the bundled Mantle/Porting Lib jars for the ones built from the Fabric Mantle 1.11 branch.

Run this after `ref/Mantle-fabric` produces `build/libs/mantle-*.jar`. It:
  1. copies the new Mantle jar into `tconstruct-fabric/libs/`, dropping the old 1.9.x jar;
  2. copies the Porting Lib jars that Mantle 1.11 depends on (2.3.16-beta.81) from the Gradle module
     cache into `tconstruct-fabric/libs/`, dropping the older 2.3.14 line;
  3. strips the Fabric metadata entries from every bundled jar's MANIFEST.MF, because Loom 1.8.13
     refuses to remap jars claiming to be built by Loom 1.12;
  4. rewrites the `jars` array of `src/main/resources/fabric.mod.json` from the `libs/` directory.

Usage: python tools/sync_mantle_libs.py [--mantle-jar path/to/mantle.jar]
"""

import glob
import io
import json
import os
import re
import shutil
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LIBS = os.path.join(ROOT, 'libs')
RESOURCES = os.path.join(ROOT, 'src', 'main', 'resources')
GRADLE_MODULES = os.path.expanduser('~/.gradle/caches/modules-2/files-2.1')

NEW_PORTING_VERSION = '2.3.16-beta.81+1.20.1'
PORTING_GROUP = 'io.github.fabricators_of_create.Porting-Lib'
OLD_PORTING_PREFIXES = ('2.3.14',)

STRIP_PREFIXES = (
    'Fabric-Loom-Version',
    'Fabric-Gradle-Version',
    'Fabric-Tiny-Remapper-Version',
    'Fabric-Mixin-',
    'Fabric-Jar-Type',
    'Fabric-Mapping-Namespace',
    'Fabric-Minecraft-Version',
    'Fabric-Loader-Version',
)


def strip_manifest(path):
    """Removes the Fabric metadata entries from a jar's manifest, rewriting the archive in place."""
    tmp = path + '.tmp'
    with zipfile.ZipFile(path) as zin, zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            data = zin.read(item.filename)
            if item.filename == 'META-INF/MANIFEST.MF':
                lines = data.decode('utf-8', 'replace').split('\r\n')
                kept = [line for line in lines if not line.startswith(STRIP_PREFIXES)]
                data = '\r\n'.join(kept).encode('utf-8')
            zout.writestr(item, data)
    shutil.move(tmp, path)


def find_gradle_jar(group_path, artifact, version, classifier=None):
    base = os.path.join(GRADLE_MODULES, group_path, artifact, version)
    pattern = '%s-%s%s.jar' % (artifact, version, '-' + classifier if classifier else '')
    hits = glob.glob(os.path.join(base, '*', pattern))
    return hits[0] if hits else None


def copy_porting_libs():
    """Copies the Porting Lib modules Mantle 1.11 needs, replacing the older bundled versions."""
    wanted = [
        'porting_lib_accessors', 'porting_lib_attributes', 'porting_lib_base', 'porting_lib_brewing',
        'porting_lib_client_events', 'porting_lib_common', 'porting_lib_config', 'porting_lib_core',
        'porting_lib_data', 'porting_lib_entity', 'porting_lib_extensions', 'porting_lib_fluids',
        'porting_lib_items', 'porting_lib_lazy_registration', 'porting_lib_loot', 'porting_lib_model_loader',
        'porting_lib_models', 'porting_lib_model_generators', 'porting_lib_networking', 'porting_lib_tags',
        'porting_lib_tool_actions', 'porting_lib_transfer', 'porting_lib_utility',
    ]
    copied = []
    for artifact in wanted:
        jar = find_gradle_jar(PORTING_GROUP.replace('.', '/'), artifact, NEW_PORTING_VERSION)
        if jar is None:
            print('  ! missing %s %s' % (artifact, NEW_PORTING_VERSION))
            continue
        target = os.path.join(LIBS, os.path.basename(jar))
        shutil.copy2(jar, target)
        copied.append(target)
    # drop the old line
    for path in glob.glob(os.path.join(LIBS, '*.jar')):
        name = os.path.basename(path)
        if any(prefix in name for prefix in OLD_PORTING_PREFIXES) and name.startswith('porting_lib'):
            os.remove(path)
            print('  removed old %s' % name)
    return copied


def write_mod_jars():
    fabric_mod = os.path.join(RESOURCES, 'fabric.mod.json')
    with io.open(fabric_mod, encoding='utf-8') as handle:
        data = json.load(handle)
    jars = []
    for path in sorted(glob.glob(os.path.join(LIBS, '*.jar'))):
        jars.append({'file': 'META-INF/jars/' + os.path.basename(path)})
    data['jars'] = jars
    deps = data.setdefault('depends', {})
    deps['fabricloader'] = '>=0.17.2'
    with io.open(fabric_mod, 'w', encoding='utf-8', newline='\n') as handle:
        json.dump(data, handle, indent=2)
        handle.write('\n')
    print('  wrote %d jar entries into fabric.mod.json' % len(jars))


def main():
    mantle_jars = glob.glob(os.path.join(os.path.dirname(ROOT), 'ref', 'Mantle-fabric', 'build', 'libs', '*.jar'))
    if '--mantle-jar' in sys.argv:
        mantle_jars = [sys.argv[sys.argv.index('--mantle-jar') + 1]]
    mantle_jars = [path for path in mantle_jars if not path.endswith('-sources.jar')]
    if not mantle_jars:
        raise SystemExit('no freshly built Mantle jar found under ref/Mantle-fabric/build/libs')

    mantle = mantle_jars[0]
    print('using Mantle jar:', mantle)
    for old in glob.glob(os.path.join(LIBS, 'Mantle-*.jar')):
        os.remove(old)
        print('  removed old %s' % os.path.basename(old))
    target = os.path.join(LIBS, os.path.basename(mantle).replace('mantle-', 'Mantle-'))
    shutil.copy2(mantle, target)
    print('  copied ->', os.path.basename(target))

    copy_porting_libs()

    for path in glob.glob(os.path.join(LIBS, '*.jar')):
        strip_manifest(path)
    print('  stripped manifests in %d jars' % len(glob.glob(os.path.join(LIBS, '*.jar'))))

    write_mod_jars()


if __name__ == '__main__':
    main()
