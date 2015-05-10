from django.conf.urls import include, url
from django.conf import settings
from django.conf.urls.static import static


urlpatterns = [
    # include the api's urls
    url(r'^api/', include('gs_web_api.urls')),
]

if settings.DEBUG:
    # hack for serving static content on the development server
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
