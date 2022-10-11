package uk.ac.ebi.biostudies.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import javax.servlet.http.HttpServletRequest;
import java.io.FileNotFoundException;

@RestController
public class View {
    private final Logger logger = LogManager.getLogger(View.class.getName());

    @RequestMapping(value = "/")
    public ModelAndView index() throws Exception {
        var mav = new ModelAndView();
        mav.setViewName("index");
        return mav;
    }

    @RequestMapping(value = {
            "/studies/help",
            "/studies/help/",
            "/{collection}/help",
            "/{collection}/help/",
            "/{collection}/.+/help",
            "/{collection}/.+/help/"
    })
    public ModelAndView help(@PathVariable(required = false) String collection) throws Exception {
        var mav = new ModelAndView();
        mav.addObject("collection", collection);
        mav.setViewName("help");
        return mav;
    }

    @RequestMapping(value = {
            "/studies",
            "/studies/",
            "/{collection}/studies",
            "/{collection}/studies/"
    }, method = RequestMethod.GET)
    public ModelAndView search(@PathVariable(required = false) String collection) throws Exception {
        var mav = new ModelAndView();
        mav.addObject("collection", collection);
        mav.setViewName("search");
        return mav;
    }

    @RequestMapping(value = {
            "/studies/{accession}",
            "/studies/{accession}/",
            "/arrays/{accession}",
            "/arrays/{accession}/",
            "/{collection}/studies/{accession}",
            "/{collection}/studies/{accession}/",
            "/{collection}/arrays/{accession}/",
            "/{collection}/arrays/{accession}"
    }, method = RequestMethod.GET)
    public ModelAndView detail(@PathVariable(required = false) String collection,
                               @PathVariable(value = "accession") String accession,
                               @RequestParam(value = "key", required = false) String key,
                               HttpServletRequest request) throws Exception {
        var mav = new ModelAndView();
        boolean isArrayExpressStudy = collection==null && (accession.toUpperCase().startsWith("E-") || accession.toUpperCase().startsWith("A-"));
        String type = accession.toUpperCase().startsWith("A-") ? "arrays":"studies";
        if(type.equals("arrays")&& request.getRequestURL().toString().contains("/studies"))
            throw new FileNotFoundException();
        mav.addObject("collection", collection);
        mav.addObject("accession", accession);
        String viewName = isArrayExpressStudy ? String.format("redirect:/arrayexpress/studies/{accession}"
                + (key != null ? "?key=" + key : ""), accession) : "detail";
        mav.setViewName(viewName);        return mav;
    }

    @RequestMapping(value = {
            "/studies/{accession}/{view}",
            "/studies/{accession}/{view}/",
            "/{collection}/studies/{accession}/{view}",
            "/{collection}/studies/{accession}/{view}/"
    }, method = RequestMethod.GET)
    public ModelAndView genericStudyView(@PathVariable(required = false) String collection,
                                         @PathVariable String accession,
                                         @PathVariable String view) throws Exception {
        var mav = new ModelAndView();
        mav.addObject("collection", collection);
        mav.addObject("accession", accession);
        mav.setViewName(view);
        return mav;
    }

    @RequestMapping(value = {
            "/{view}",
            "/{view}/",
    }, method = RequestMethod.GET)
    public ModelAndView genericView(@PathVariable String view) throws Exception {
        var mav = new ModelAndView();
        mav.setViewName(view);
        return mav;
    }

    @RequestMapping(value = {"/{collection}/studies/EMPIAR-{id:.+}", "/studies/EMPIAR-{id:.+}"})
    public RedirectView redirectEMPIAR(@PathVariable(required = false) String collection, @PathVariable String id) {
        RedirectView redirectView = new RedirectView();
        redirectView.setUrl("https://www.ebi.ac.uk/empiar/" + id);
        return redirectView;
    }

}