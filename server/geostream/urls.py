from django.conf.urls import url, include
from rest_framework import routers
from api import views
from django.conf import settings
from django.conf.urls.static import static


router = routers.DefaultRouter()
router.register(r'users', views.UserViewSet)
router.register(r'posts', views.PostViewSet)

# Wire up our API using automatic URL routing.
# Additionally, we include login URLs for the browsable API.
urlpatterns = [
    url(r'^api/', include(router.urls)),
]

if settings.DEBUG:
    # for serving images when on the development server
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
