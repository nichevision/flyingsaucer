package org.xhtmlrenderer.swing;

@SuppressWarnings("serial")
public class UriNotImmediatelyAvailableException extends RuntimeException
{
	private final String uri;
	
	public UriNotImmediatelyAvailableException(final String uri)
	{
		this.uri = uri;
	}
	
	public String getUri()
	{
		return this.uri;
	}
}
