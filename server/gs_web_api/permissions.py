from rest_framework import permissions
from django.conf import settings


class ClientSecretPermission(permissions.BasePermission):
    """
    Permission that determines whether a Post can be deleted by a client. In order to delete a post the client must
    provide a ClientSecret header that matches the client_secret field in the Post object that they are trying to
    delete.
    """
    META_KEY = 'HTTP_CLIENTSECRET'

    def has_object_permission(self, request, view, obj):
        if request.method != 'DELETE':
            return True  # this permission only handles deletions

        if self.META_KEY not in request.META:
            return False  # no client secret provided
        client_secret = request.META[self.META_KEY]

        return str(obj.client_secret) == str(client_secret)  # check if the client secrets match


class UserAgentPermission(permissions.BasePermission):
    """
    Permission that limits the User-Agents that are permitted.
    """
    META_KEY = 'HTTP_USER_AGENT'
    ALLOWED_USER_AGENTS = ('Geostream/1 (Android)',)

    def has_permission(self, request, view):
        if not settings.GS_WEB_API['CHECK_USER_AGENT']:
            return True
        return self.META_KEY in request.META and request.META[self.META_KEY] in self.ALLOWED_USER_AGENTS
