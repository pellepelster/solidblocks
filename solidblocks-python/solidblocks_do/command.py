import json
import os
import pty
import select
import subprocess
import sys
import termios
import tty

from solidblocks_do.log import log_divider_thin, log_divider_top, log_divider_bottom, log_success, log_error


# see https://pellepelster.github.io/solidblocks/python/command/#command_run
def command_ensure_exists(command):
    if not subprocess.run(['which', command], stderr=subprocess.DEVNULL, stdout=subprocess.DEVNULL).returncode == 0:
        log_error(f"command '{command}' not found")
        return False
    return True


def command_run_interactive(command, env=None, workdir=None):
    log_divider_top()
    log_success(f"running command '{' '.join(command)}'")
    log_divider_thin()
    # save original tty setting then set it to raw mode
    old_tty = termios.tcgetattr(sys.stdin)
    tty.setraw(sys.stdin.fileno())

    # open pseudo-terminal to interact with subprocess
    master_fd, slave_fd = pty.openpty()

    # use os.setsid() make it run in a new process group, or bash job control will not be enabled
    p = subprocess.Popen(command,
                         preexec_fn=os.setsid,
                         stdin=slave_fd,
                         stdout=slave_fd,
                         stderr=slave_fd,
                         env=env,
                         cwd=workdir,
                         universal_newlines=True)

    while p.poll() is None:
        r, w, e = select.select([sys.stdin, master_fd], [], [], 1)
        if sys.stdin in r:
            d = os.read(sys.stdin.fileno(), 10240)
            os.write(master_fd, d)
        elif master_fd in r:
            o = os.read(master_fd, 10240)
            if o:
                os.write(sys.stdout.fileno(), o)

    # restore tty settings back
    termios.tcsetattr(sys.stdin, termios.TCSADRAIN, old_tty)
    log_divider_bottom()

    return p.returncode == 0


# see https://pellepelster.github.io/solidblocks/python/command/#command_run
def command_run(command, env=None, workdir=None, shell=True):
    log_divider_top()

    command_log = command
    if type(command) is list:
        command_log = ' '.join(command)

    if workdir:
        log_success(f"running command '{command_log}' in '{workdir}'")
    else:
        log_success(f"running command '{command_log}'")
    log_divider_thin()

    command_env = {**(env or {}), **dict(os.environ)}

    try:
        result = subprocess.run(command_log, env=command_env, cwd=workdir, shell=shell)
        return result.returncode == 0
    except FileNotFoundError:
        return False
    finally:
        log_divider_bottom()


def command_exec(command, env=None, workdir=None):
    command_env = dict(os.environ)

    if env is not None:
        command_env.update(env)

    proc = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=command_env, cwd=workdir)
    stdout = proc.stdout.read()
    stderr = proc.stderr.read()
    proc.wait()

    return proc.returncode, stdout.decode("utf-8"), stderr.decode("utf-8")


# see https://pellepelster.github.io/solidblocks/python/command/#command_exec_json
def command_exec_json(command, env=None, workdir=None):
    _, stdout, _ = command_exec(command, env, workdir)
    try:
        return json.loads(stdout)
    except:
        return None
