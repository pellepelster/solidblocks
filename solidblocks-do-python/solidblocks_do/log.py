import logging

import colorlog


def log_default_logger(name=None):
    logging.basicConfig()
    logging.getLogger().setLevel(logging.INFO)

    handler = colorlog.StreamHandler()

    if name is None:
        handler.setFormatter(colorlog.ColoredFormatter('%(log_color)s%(levelname)s %(message)s'))
    else:
        handler.setFormatter(colorlog.ColoredFormatter('%(log_color)s[%(name)s] %(levelname)s %(message)s'))

    new_logger = colorlog.getLogger(name)
    new_logger.addHandler(handler)
    return new_logger


logger = log_default_logger()


def log_divider_bold():
    print("█" * 120)


def log_divider_normal():
    print("▄" * 120)


def log_divider_thin():
    print("─" * 120)
