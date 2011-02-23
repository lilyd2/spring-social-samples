/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.social.showcase.signup;

import java.io.Serializable;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.social.showcase.ShowcaseUser;
import org.springframework.social.showcase.UserRepository;
import org.springframework.social.showcase.UsernameAlreadyInUseException;
import org.springframework.social.web.connect.ProviderSignInAccount;
import org.springframework.social.web.connect.ServiceProviderLocator;
import org.springframework.social.web.connect.SignInService;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.WebRequest;

@Controller
public class SignupController implements BeanFactoryAware {

	private final UserRepository userRepository;

	private final SignInService signinService;

	private ServiceProviderLocator serviceProviderLocator;

	@Inject
	public SignupController(UserRepository userRepository, SignInService signinService) {
		this.userRepository = userRepository;
		this.signinService = signinService;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		serviceProviderLocator = new ServiceProviderLocator((ListableBeanFactory) beanFactory);
	}

	@RequestMapping(value = "/signup", method = RequestMethod.GET)
	public SignupForm signupForm() {
		return new SignupForm();
	}

	@RequestMapping(value = "/signup", method = RequestMethod.POST)
	public String signup(@Valid SignupForm form, BindingResult formBinding, WebRequest request) {
		if (formBinding.hasErrors()) {
			return null;
		}
		boolean userCreated = createUser(form, formBinding);
		if (userCreated) {
			createConnection(request, form.getUsername());
			return "redirect:/";
		}

		return null;
	}

	private boolean createUser(SignupForm form, BindingResult formBinding) {
		try {
			ShowcaseUser user = new ShowcaseUser(form.getUsername(), form.getPassword(),
					form.getFirstName(), form.getLastName());
			userRepository.createUser(user);

			signinService.signIn(user.getUsername());
			return true;
		} catch (UsernameAlreadyInUseException e) {
			formBinding.rejectValue("username", "user.duplicateUsername", "already in use");
			return false;
		}
	}

	private void createConnection(WebRequest request, Serializable accountId) {
		ProviderSignInAccount signInAccount = (ProviderSignInAccount) request.getAttribute("providerSignInAccount", WebRequest.SCOPE_SESSION);
		if(signInAccount != null) {
			request.removeAttribute("providerSignInAccount", WebRequest.SCOPE_SESSION);
			signInAccount.connect(serviceProviderLocator, accountId);
		}
	}

}
