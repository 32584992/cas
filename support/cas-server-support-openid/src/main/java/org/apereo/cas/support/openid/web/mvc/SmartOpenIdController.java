package org.apereo.cas.support.openid.web.mvc;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.support.openid.OpenIdProtocolConstants;
import org.apereo.cas.web.AbstractDelegateController;
import org.openid4java.message.Message;
import org.openid4java.message.ParameterList;
import org.openid4java.server.ServerManager;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates an association to an openid association request.
 *
 * @author Frederic Esnault
 * @since 3.5
 */
@Slf4j
@AllArgsConstructor
public class SmartOpenIdController extends AbstractDelegateController implements Serializable {
    private static final long serialVersionUID = -594058549445950430L;

    private final transient ServerManager serverManager;
    private final transient View successView;

    /**
     * Gets the association response. Determines the mode first.
     * If mode is set to associate, will set the response. Then
     * builds the response parameters next and returns.
     *
     * @param request the request
     * @return the association response
     */
    public Map<String, String> getAssociationResponse(final HttpServletRequest request) {
        final var parameters = new ParameterList(request.getParameterMap());

        final var mode = parameters.hasParameter(OpenIdProtocolConstants.OPENID_MODE)
            ? parameters.getParameterValue(OpenIdProtocolConstants.OPENID_MODE)
            : null;

        Message response = null;

        if (StringUtils.equals(mode, OpenIdProtocolConstants.ASSOCIATE)) {
            response = this.serverManager.associationResponse(parameters);
        }
        final Map<String, String> responseParams = new HashMap<>();
        if (response != null) {
            responseParams.putAll(response.getParameterMap());
        }

        return responseParams;

    }

    @Override
    protected ModelAndView handleRequestInternal(final HttpServletRequest request, final HttpServletResponse response) {
        final Map<String, String> parameters = new HashMap<>(getAssociationResponse(request));
        return new ModelAndView(this.successView, parameters);
    }

    @Override
    public boolean canHandle(final HttpServletRequest request, final HttpServletResponse response) {
        final var openIdMode = request.getParameter(OpenIdProtocolConstants.OPENID_MODE);
        if (StringUtils.equals(openIdMode, OpenIdProtocolConstants.ASSOCIATE)) {
            LOGGER.info("Handling request. openid.mode : [{}]", openIdMode);
            return true;
        }
        LOGGER.info("Cannot handle request. openid.mode : [{}]", openIdMode);
        return false;
    }
}
