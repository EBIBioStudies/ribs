package uk.ac.ebi.biostudies.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.ac.ebi.biostudies.service.SubmissionNotAccessibleException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;

/** Created by ehsan on 10/04/2017. */
@ControllerAdvice
public class GeneralPurposeExceptionHandler extends ResponseEntityExceptionHandler {
  private final Logger logger = LogManager.getLogger(GeneralPurposeExceptionHandler.class.getName());

  @ExceptionHandler(value = {SubmissionNotAccessibleException.class})
  @ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "Forbidden")
  public ModelAndView resolveException(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      SubmissionNotAccessibleException ex) {
    logger.error(ex.getMessage(), ex);
    return new ModelAndView("detail");
  }

  @ExceptionHandler(value = {FileNotFoundException.class})
  @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "File not found")
  public ModelAndView resolveException(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      FileNotFoundException ex) {
    logger.error(ex.getMessage(), ex);
    return new ModelAndView("detail");
  }

  @ExceptionHandler(value = {Exception.class})
  @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Internal Server Error")
  public ModelAndView resolveException(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    logger.error(ex.getMessage(), ex);
    return new ModelAndView("detail");
  }
}
