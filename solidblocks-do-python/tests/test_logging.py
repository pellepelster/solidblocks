from solidblocks_do.log import log_divider_bold, log_divider_thin, log_divider_bottom, log_divider_top, log_info, \
    log_ok, log_warning, log_error, log_hint


def test_logger():
    log_info("some info message")
    log_ok("some ok message")
    log_warning("some warning message")
    log_error("some error message")
    log_hint("some hint message")


def test_dividers():
    log_divider_bold()
    log_divider_top()
    log_divider_bottom()
    log_divider_thin()
