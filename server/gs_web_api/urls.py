from django.conf.urls import url, include
from rest_framework import routers
from . import views


# add views to the router
router = routers.DefaultRouter()
router.register(r'posts', views.PostViewSet)

urlpatterns = [
    # include URLs created by the router
    url(r'^', include(router.urls)),
]
