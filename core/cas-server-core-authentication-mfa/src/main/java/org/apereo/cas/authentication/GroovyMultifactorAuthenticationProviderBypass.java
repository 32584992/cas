package org.apereo.cas.authentication;

import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.configuration.model.support.mfa.MultifactorAuthenticationProviderBypassProperties;
import org.apereo.cas.configuration.model.support.mfa.MultifactorAuthenticationProviderBypassProperties.ExecuteDefaultOptions;
import org.apereo.cas.services.MultifactorAuthenticationProvider;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.util.ScriptingUtils;
import org.springframework.core.io.Resource;

import javax.servlet.http.HttpServletRequest;

/**
 * This is {@link GroovyMultifactorAuthenticationProviderBypass}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@Slf4j
public class GroovyMultifactorAuthenticationProviderBypass extends DefaultMultifactorAuthenticationProviderBypass {
    private static final long serialVersionUID = -4909072898415688377L;

    private final transient Resource groovyScript;

    public GroovyMultifactorAuthenticationProviderBypass(final MultifactorAuthenticationProviderBypassProperties bypass) {
        super(bypass);
        this.groovyScript = bypass.getGroovy().getLocation();
    }

    @Override
    public boolean shouldMultifactorAuthenticationProviderExecute(final Authentication authentication,
                                                                  final RegisteredService registeredService,
                                                                  final MultifactorAuthenticationProvider provider,
                                                                  final HttpServletRequest request) {
        try {
            final Principal principal = authentication.getPrincipal();
            LOGGER.debug("Evaluating multifactor authentication bypass properties for principal [{}], "
                    + "service [{}] and provider [{}] via Groovy script [{}]",
                principal.getId(), registeredService, provider, this.groovyScript);
            boolean defaultBypass = false;
            if (bypassProperties.getGroovy().getExecuteDefault() == ExecuteDefaultOptions.BEFORE) {
                defaultBypass = super.shouldMultifactorAuthenticationProviderExecute(authentication, registeredService, provider, request);
            }
            final boolean isBypassed = ScriptingUtils.executeGroovyScript(this.groovyScript,
                new Object[]{authentication, principal, registeredService, provider, LOGGER, request}, Boolean.class);
            if (isBypassed) {
                LOGGER.info("Groovy bypass script determined [{}] would be passed for [{}]", principal.getId(), provider.getId());
                updateAuthenticationToRememberBypass(authentication, provider, principal);
            } else {
                updateAuthenticationToForgetBypass(authentication, provider, principal);
            }
            if (bypassProperties.getGroovy().getExecuteDefault() == ExecuteDefaultOptions.AFTER) {
                defaultBypass = super.shouldMultifactorAuthenticationProviderExecute(authentication, registeredService, provider, request);
            }
            return isBypassed || defaultBypass;
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return super.shouldMultifactorAuthenticationProviderExecute(authentication, registeredService, provider, request);
    }
}
