import os
import pty
import select
import subprocess
import sys
import termios
import tty

from solidblocks_do.log import log_divider_thin, log_divider_top, log_divider_bottom, log_ok, log_error


def command_exists(command):
    if not subprocess.run(['which', command], stderr=subprocess.DEVNULL, stdout=subprocess.DEVNULL).returncode == 0:
        log_error(f"command '{command}' not found")
        return False
    return True


def command_run_interactive(command, env=None, workdir=None):
    log_divider_top()
    log_ok(f"running command '{' '.join(command)}'")
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


def command_run(command, env=None, workdir=None):
    log_divider_top()
    if workdir:
        log_ok(f"running command '{' '.join(command)}' in '{workdir}'")
    else:
        log_ok(f"running command '{' '.join(command)}'")
    log_divider_thin()
    try:
        proc = subprocess.Popen(command, env=env, cwd=workdir)
        proc.wait()
        return proc.returncode == 0
    except FileNotFoundError:
        return False
    finally:
        log_divider_bottom()
        print()


def command_exec(command, env=None, workdir=None):
    proc = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=env, cwd=workdir)
    stdout = proc.stdout.read()
    stderr = proc.stderr.read()
    proc.wait()

    return proc.returncode, stdout.decode("utf-8"), stderr.decode("utf-8")
