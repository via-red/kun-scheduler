/**
 * Umi application route definition
 * @author Josh Ouyang 06/29/2020
 */

export const appRoutes = [
  {
    path: '/',
    component: '@/layouts/index',
    title: 'common.pageTitle.homepage',
    routes: [
      {
        path: '/',
        exact: true,
        menuDisplay: false,
        component: 'home/index',
        wrappers: ['@/wrappers/path', '@/wrappers/isLogin'],
      },
      {
        title: 'common.pageTitle.dataDiscovery',
        path: '/data-discovery',
        icon: 'FileTextOutlined',
        menuDisplay: true,
        showChildren: false,
        breadcrumbLink: true,
        permissions: ['DATA_DISCOVERY'],
        routes: [
          {
            title: 'common.pageTitle.dataDiscovery',
            path: '.',
            component: 'data-discovery/index',
            exact: true,
            breadcrumbLink: true,
            permissions: ['DATA_DISCOVERY'],
            wrappers: [
              '@/wrappers/path',
              '@/wrappers/isLogin',
              '@/wrappers/permission',
            ],
          },
          {
            title: 'common.pageTitle.datasets',
            path: '/data-discovery/dataset',
            menuDisplay: true,
            icon: 'SnippetsOutlined',
            showChildren: false,
            breadcrumbLink: true,
            permissions: ['DATA_DISCOVERY'],
            routes: [
              {
                title: 'common.pageTitle.datasets',
                path: '.',
                component: 'dataset/index',
                breadcrumbLink: true,
                exact: true,
                permissions: ['DATA_DISCOVERY'],
                wrappers: [
                  '@/wrappers/path',
                  '@/wrappers/isLogin',
                  '@/wrappers/permission',
                ],
              },
              {
                title: 'common.pageTitle.datasetDetail',
                path: '/data-discovery/dataset/:datasetId',
                component: 'dataset/dataset-detail/index',
                breadcrumbLink: true,
                exact: true,
                permissions: ['DATA_DISCOVERY'],
                wrappers: [
                  '@/wrappers/path',
                  '@/wrappers/isLogin',
                  '@/wrappers/permission',
                ],
              },
            ],
          },
          {
            title: 'common.pageTitle.glossary',
            path: '/data-discovery/glossary',
            menuDisplay: true,
            icon: 'SnippetsOutlined',
            showChildren: false,
            breadcrumbLink: true,
            routes: [
              {
                title: 'common.pageTitle.glossary',
                path: '.',
                component: 'glossary/index',
                breadcrumbLink: true,
                exact: true,
                permissions: ['DATA_DISCOVERY'],
                wrappers: [
                  '@/wrappers/path',
                  '@/wrappers/isLogin',
                  '@/wrappers/permission',
                ],
              },
              {
                title: 'common.pageTitle.glossaryCreate',
                path: '/data-discovery/glossary/create',
                component: 'glossary/glossary-detail/index',
                breadcrumbLink: true,
                exact: true,
                permissions: ['DATA_DISCOVERY'],
                wrappers: [
                  '@/wrappers/path',
                  '@/wrappers/isLogin',
                  '@/wrappers/permission',
                ],
              },
              {
                title: 'common.pageTitle.glossaryDetail',
                path: '/data-discovery/glossary/:glossaryId',
                component: 'glossary/glossary-detail/index',
                breadcrumbLink: true,
                exact: true,
                permissions: ['DATA_DISCOVERY'],
                wrappers: [
                  '@/wrappers/path',
                  '@/wrappers/isLogin',
                  '@/wrappers/permission',
                ],
              },
            ],
          },
        ],
      },
      {
        title: 'common.pageTitle.dataDevelopment',
        path: '/data-development',
        icon: 'ApartmentOutlined',
        permissions: ['DATA_DEVELOPMENT'],
        menuDisplay: true,
        routes: [
          {
            title: 'common.pageTitle.dataDevelopment',
            path: '.',
            exact: true,
            component: '@/pages/data-development/index',
            permissions: ['DATA_DEVELOPMENT'],
            wrappers: [
              '@/wrappers/path',
              '@/wrappers/isLogin',
              '@/wrappers/withDnDContext',
              '@/wrappers/permission',
            ],
          },
          {
            title: 'common.pageTitle.taskDefinition',
            path: '/data-development/task-definition/:taskDefId',
            component: 'data-development/task-definition-config',
            exact: true,
            permissions: ['DATA_DEVELOPMENT'],
            wrappers: [
              '@/wrappers/path',
              '@/wrappers/isLogin',
              '@/wrappers/permission',
            ],
          },
        ],
      },
      {
        title: 'common.pageTitle.operationCenter',
        menuDisplay: true,
        path: '/operation-center',
        icon: 'ToolOutlined',
        permissions: ['DATA_DEVELOPMENT'],
        showChildren: true,
        routes: [
          {
            title: 'common.pageTitle.operationCenter.scheduledTasks',
            path: './scheduled-tasks',
            menuDisplay: true,
            icon: 'CalendarOutlined',
            routes: [
              {
                path: '.',
                exact: true,
                component: '@/pages/operation-center/scheduled-tasks',
                permissions: ['DATA_DEVELOPMENT'],
                wrappers: [
                  '@/wrappers/path',
                  '@/wrappers/isLogin',
                  '@/wrappers/permission',
                ],
              },
              {
                title: 'common.pageTitle.operationCenter.scheduledTasks',
                menuDisplay: false,
                path: './:id',
                component: '@/pages/operation-center/deployed-task-detail',
                exact: true,
                permissions: ['DATA_DEVELOPMENT'],
                wrappers: [
                  '@/wrappers/path',
                  '@/wrappers/isLogin',
                  '@/wrappers/permission',
                ],
              },
            ],
          },
        ],
      },
      {
        title: 'common.pageTitle.dataSettings',
        path: '/data-settings',
        menuDisplay: true,
        icon: 'SettingOutlined',
        component: 'data-settings/index',
        breadcrumbLink: true,
        permissions: ['DATA_DISCOVERY'],
        wrappers: [
          '@/wrappers/path',
          '@/wrappers/isLogin',
          '@/wrappers/permission',
        ],
      },
      {
        title: 'common.pageTitle.login',
        path: '/login',
        component: 'login/index',
        exact: true,
        wrappers: ['@/wrappers/path', '@/wrappers/isLogin'],
      },

      {
        path: '*',
        component: 'home/index',
        wrappers: [
          '@/wrappers/path',
          '@/wrappers/isLogin',
          '@/wrappers/permission',
        ],
      },
    ],
  },
];
