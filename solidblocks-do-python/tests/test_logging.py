from solidblocks_do.log import log_default_logger, log_divider_bold, log_divider_thin, log_divider_normal

logger = log_default_logger()


def test_logger():
    logger.info("some info message")
    logger.warning("some warning message")
    logger.error("some error message")


def test_dividers():
    log_divider_bold()
    log_divider_normal()
    log_divider_thin()


logger_with_name = log_default_logger('name1')


def test_logger_with_name():
    logger_with_name.info("some info message")
    logger_with_name.warning("some warning message")
    logger_with_name.error("some error message")
