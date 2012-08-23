<%@ page contentType="text/plain" %>
<%
    boolean handled = false; // Set to true after handling the error
    if(pageContext != null) {
        ErrorData ed = null;
        try {
            ed = pageContext.getErrorData();
        } catch(NullPointerException ne) {
            // If the error page was accessed directly, a NullPointerException
            // is thrown at (PageContext.java:514).
            // Catch and ignore it... it effectively means we can't use the ErrorData
        }
        if(ed != null) {
            // (this should show error code 404, and the name of the missing page)
            out.println("ErrorCode: " + ed.getStatusCode());
//            out.println("URL: " + ed.getRequestURI());

            // Error handled successfully, set a flag
            handled = true;
        }
    }

    // Check if the error was handled
    if(!handled) {
        out.println("Unknown Error");
    }
%>
