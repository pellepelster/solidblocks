from solidblocks_do.log import log_divider_bold, log_divider_thin, log_divider_bottom, log_divider_top, log_info, \
    log_success, log_warning, log_error, log_hint


def test_logger():
    print("\n")
    log_info("some info message")
    log_success("some success message")
    log_warning("some warning message")
    log_error("some error message")
    log_hint("some hint message")
    print("\n")


def test_dividers():
    print("\nlog_divider_bold")
    log_divider_bold()
    print("\n\nlog_divider_top")
    log_divider_top()
    print("\n\nlog_divider_bottom")
    log_divider_bottom()
    print("\n\nlog_divider_thin")
    log_divider_thin()
