class colors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKCYAN = '\033[96m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    RESET = '\033[0m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'


def log_ok(message):
    print(f"{colors.OKGREEN}{message}{colors.RESET}")


def log_hint(message):
    print(f"{colors.OKBLUE}{message}{colors.RESET}")


def log_info(message):
    print(f"{message}")


def log_warning(message):
    print(f"{colors.WARNING}{message}{colors.RESET}")


def log_error(message):
    print(f"{colors.FAIL}{message}{colors.RESET}")


def log_divider_bold():
    print("█" * 120, flush=True)


def log_divider_top():
    print("▀" * 120, flush=True)


def log_divider_bottom():
    print("▄" * 120, flush=True)


def log_divider_thin():
    print("─" * 120, flush=True)
