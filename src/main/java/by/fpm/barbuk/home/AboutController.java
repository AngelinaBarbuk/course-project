package by.fpm.barbuk.home;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
class AboutController {

	@ModelAttribute("module")
	String module() {
		return "about";
	}

	@GetMapping("/about")
	String about() {
		return "home/about";
	}
}
