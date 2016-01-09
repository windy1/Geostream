from rest_framework import viewsets
from .models import Post, Comment, Flag
from .serializers import PostSerializer, CommentSerializer, FlagSerializer
from rest_framework import status
from rest_framework.decorators import list_route
from rest_framework.response import Response
from .permissions import ClientSecretPermission, UserAgentPermission


def inject_client_secret(serializer):
    """
    Injects a models 'client_secret' field into a request response. Client secrets are only returned on the initial
    creation of an object.
    """
    if (serializer.is_valid()):
        obj = serializer.save()
        data = serializer.data
        data['client_secret'] = obj.client_secret
        return Response(data)
    else:
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


class PostViewSet(viewsets.ModelViewSet):
    queryset = Post.objects.all()
    serializer_class = PostSerializer
    http_method_names = ['get', 'post', 'head', 'delete']
    permission_classes = (ClientSecretPermission, UserAgentPermission,)

    @list_route()
    def range(self, request):
        """
        Returns a list view of the posts within a specified lat/lng range.
        """
        data = request.query_params
        # make sure range parameters are present
        if 'fromLat' not in data or 'toLat' not in data or 'fromLng' not in data or 'toLng' not in data:
            return Response(status=status.HTTP_400_BAD_REQUEST)

        fromLat = float(data['fromLat'])
        toLat = float(data['toLat'])
        fromLng = float(data['fromLng'])
        toLng = float(data['toLng'])

        # find posts that are within the range
        posts = Post.objects.all()
        inRange = []
        for post in posts:
            if post.lat >= fromLat and post.lat <= toLat and post.lng >= fromLng and post.lng <= toLng:
                # post is in range
                if post.is_expired():
                    post.delete()  # delete in-range post if expired
                else:
                    inRange.append(post)  # otherwise, add to response list

        # return list of posts
        serializer = self.get_serializer(inRange, many=True)
        return Response(serializer.data)

    def list(self, request):
        # delete all expired posts
        posts = Post.objects.all()
        for post in posts:
            if post.is_expired():
                post.delete()
        return viewsets.ModelViewSet.list(self, request)  # after deleting old posts, pass to super method

    def create(self, request):
        # include the client_secret only on initial creation
        return inject_client_secret(PostSerializer(data=request.data))


class CommentViewSet(viewsets.ModelViewSet):
    queryset = Comment.objects.all()
    serializer_class = CommentSerializer
    http_method_names = ['get', 'post', 'head', 'delete']
    permission_classes = (ClientSecretPermission, UserAgentPermission,)

    def create(self, request):
        # include the client_secret only on initial creation
        return inject_client_secret(CommentSerializer(data=request.data))


class FlagViewSet(viewsets.ModelViewSet):
    queryset = Flag.objects.all()
    serializer_class = FlagSerializer
    http_method_names = ['get', 'post', 'head']
    permission_classes = (UserAgentPermission,)
