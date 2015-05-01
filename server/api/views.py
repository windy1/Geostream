from rest_framework import viewsets, generics, status
from rest_framework.decorators import api_view, permission_classes, authentication_classes
from rest_framework.response import Response
from rest_framework.permissions import AllowAny
from django.contrib.auth.models import User
from .models import Post
from .serializers import PostSerializer, UserSerializer


class UserViewSet(viewsets.ModelViewSet):
    serializer_class = UserSerializer
    queryset = User.objects.all();
    # makes URLs formatted like '/api/users/<username>/'
    lookup_field = 'username'


class PostViewSet(viewsets.ModelViewSet):
    queryset = Post.objects.all().select_related('user')
    serializer_class = PostSerializer


@api_view(['POST'])
@authentication_classes(())
@permission_classes((AllowAny,))
def signup_user(request):
    serialized = UserSerializer(data=request.data, context={'request': request})
    # check if the request was valid
    if serialized.is_valid():
        serialized.save()
        return Response(serialized.data, status=status.HTTP_201_CREATED)

    # bad request
    return Response(serialized._errors, status=status.HTTP_400_BAD_REQUEST)
