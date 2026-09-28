"""Run server regressions against production sources without an Android SDK.

Only Android/provider infrastructure, logging and the Application are replaced.
The real commands, file adapters, settings and socket workers are compiled.
"""
import argparse
import os
from pathlib import Path
import re
import shutil
import subprocess
import urllib.request

ROOT = Path(__file__).resolve().parents[2]
TESTS = Path(__file__).resolve().parent
BUILD = ROOT / 'build' / 'jvm-regression'
PRODUCTION = ROOT / 'app/src/main/java/com/jhonju/ps3netsrv'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--json-jar', type=Path, help='Optional local org.json jar')
    args = parser.parse_args()
    BUILD.mkdir(parents=True, exist_ok=True)
    classes = BUILD / 'classes'
    classes.mkdir(exist_ok=True)
    json_jar = args.json_jar
    if json_jar is None:
        cache = Path(os.environ.get('GRADLE_USER_HOME', str(Path.home() / '.gradle')))
        matches = list((cache / 'caches/modules-2/files-2.1/org.json/json').glob('20180813/*/*.jar'))
        json_jar = matches[0] if matches else BUILD / 'json-20180813.jar'
        if not json_jar.exists():
            urllib.request.urlretrieve(
                'https://repo.maven.apache.org/maven2/org/json/json/20180813/json-20180813.jar', json_jar)

    sources = [p for p in (PRODUCTION / 'server').rglob('*.java') if p.name != 'FileLogger.java']
    sources += [PRODUCTION / 'app/SettingsService.java']
    resources = {}
    for path in sources:
        for group, name in re.findall(r'R\.(\w+)\.(\w+)', path.read_text(encoding='utf-8')):
            resources.setdefault(group, set()).add(name)
    r_source = BUILD / 'R.java'
    r_source.write_text('package com.jhonju.ps3netsrv; public class R {\n' + ''.join(
        'public static class ' + group + ' {\n' + ''.join(
            'public static final int ' + name + '=' + str(i + 1) + ';\n'
            for i, name in enumerate(sorted(names))) + '}\n'
        for group, names in resources.items()) + '}\n', encoding='utf-8')
    sources += [r_source] + list((TESTS / 'stubs').rglob('*.java')) + list((TESTS / 'src').rglob('*.java'))

    java_home = os.environ.get('JAVA_HOME')
    suffix = '.exe' if os.name == 'nt' else ''
    javac = str(Path(java_home) / 'bin' / ('javac' + suffix)) if java_home else shutil.which('javac')
    java = str(Path(java_home) / 'bin' / ('java' + suffix)) if java_home else shutil.which('java')
    if not javac or not java:
        raise SystemExit('A JDK 11 or newer is required; set JAVA_HOME.')
    compile_args = ['--release', '8', '-encoding', 'UTF-8', '-cp', str(json_jar), '-d', str(classes)]
    compile_args += [str(p) for p in sources]
    argfile = BUILD / 'javac.args'
    argfile.write_text('\n'.join('"' + s.replace('\\', '/') + '"' for s in compile_args), encoding='utf-8')
    subprocess.run([javac, '@' + str(argfile)], check=True)
    for test in ['ProtocolRegressionTest', 'FileRegressionTest', 'StateRegressionTest']:
        subprocess.run([java, '-Xmx512m', '-cp', str(classes) + os.pathsep + str(json_jar),
                        test, str(BUILD)], check=True)


if __name__ == '__main__':
    main()
